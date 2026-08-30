/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.dynamicbeans;

import java.util.Map;

import groovy.lang.GString;
import step.expressions.ExpressionHandler;
import step.expressions.ProtectedVariable;

public class DynamicValueResolver {

    private final ExpressionHandler expressionHandler;
    private final StringInterpolator stringInterpolator;

    public DynamicValueResolver(ExpressionHandler expressionHandler) {
        this(expressionHandler, new StringInterpolator(expressionHandler));
    }

    public DynamicValueResolver(ExpressionHandler expressionHandler, StringInterpolator stringInterpolator) {
        super();
        this.expressionHandler = expressionHandler;
        this.stringInterpolator = stringInterpolator;
    }

    public void evaluate(DynamicValue<?> dynamicValue, Map<String, Object> bindings) {
        evaluate(dynamicValue, bindings, true);
    }

    /**
     * @param interpolatePlainValue whether the expressions contained in a plain (non dynamic) string value are
     *                              interpolated. False for the values holding a structured document, whose
     *                              expressions are resolved by the resolver of that document instead. See
     *                              {@link NoStringInterpolation}
     */
    public void evaluate(DynamicValue<?> dynamicValue, Map<String, Object> bindings, boolean interpolatePlainValue) {
        if (dynamicValue.isDynamic()) {
            dynamicValue.evalutationResult = getEvaluationResult(dynamicValue.expression, bindings, dynamicValue.hasProtectedAccess());
        } else {
            // A null result means that the value requires no interpolation and is to be returned as is
            dynamicValue.setInterpolationResult(interpolatePlainValue ? interpolateIfRequired(dynamicValue, bindings) : null);
        }
    }

    private EvaluationResult interpolateIfRequired(DynamicValue<?> dynamicValue, Map<String, Object> bindings) {
        EvaluationResult result = new EvaluationResult();
        try {
            StringInterpolator.Result interpolation = stringInterpolator.interpolate(dynamicValue.value, bindings, dynamicValue.hasProtectedAccess());
            if (interpolation == null) {
                return null;
            }
            // The obfuscated value is identical to the clear one unless a protected variable was interpolated
            result.setResultValue(interpolation.getObfuscatedValue());
            result.setProtectedValue(interpolation.getValue());
        } catch (Exception e) {
            result.setEvaluationException(e);
        }
        return result;
    }

    private EvaluationResult getEvaluationResult(String expression, Map<String, Object> bindings, boolean hasProtectedAccess) {
        EvaluationResult result = new EvaluationResult();
        try {
            Object evaluationResult;
            Object protectedResult = null;

            Object o = expressionHandler.evaluateGroovyExpression(expression, bindings, hasProtectedAccess);
            //If the result is a ProtectedVariable and access is granted, the clear value is added as protectedResult of the evaluation result
            if (hasProtectedAccess && o instanceof ProtectedVariable) {
                ProtectedVariable pb = (ProtectedVariable) o;
                protectedResult = pb.value;
                evaluationResult = pb.obfuscatedValue;
            } else {
                //Otherwise the result is unchanged.
                // This means that ProtectedVariable can be returned when calling DynamicValue.get() which must be handled
                // carefully (but remain in a controlled and safe context). The only current use case is for the expression "dataSet.next" for protected dataset
                // when used in a Set control, the ProtectedVariable is added to the variables and thus protected when accessed in following groovy expressions
                evaluationResult = o;
            }

            evaluationResult = convertResultIfRequired(evaluationResult);
            protectedResult = (protectedResult == null) ? evaluationResult : convertResultIfRequired(protectedResult);
            result.setResultValue(evaluationResult);
            result.setProtectedValue(protectedResult);
        } catch (Exception e) {
            result.setEvaluationException(e);
        }
        return result;
    }

    /**
     * When using placeholders in strings, groovy returns an object of type GString.
     * For this reason we call the toString() method here to avoid later casting issues when DynamicValue.get() is called
     *
     * @param result the result object
     * @return converted result
     */
    private static Object convertResultIfRequired(Object result) {
        return (result instanceof GString) ? result.toString() : result;
    }

}
