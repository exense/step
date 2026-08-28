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
    private final boolean stringInterpolationEnabled;

    public DynamicValueResolver(ExpressionHandler expressionHandler) {
        this(expressionHandler, true);
    }

    /**
     * @param expressionHandler          the handler used to evaluate the groovy expressions
     * @param stringInterpolationEnabled whether the expressions contained in plain (non dynamic) string values
     *                                   are interpolated. When disabled, plain values are always returned as is
     */
    public DynamicValueResolver(ExpressionHandler expressionHandler, boolean stringInterpolationEnabled) {
        super();
        this.expressionHandler = expressionHandler;
        this.stringInterpolationEnabled = stringInterpolationEnabled;
    }

    public void evaluate(DynamicValue<?> dynamicValue, Map<String, Object> bindings) {
        if (dynamicValue.isDynamic()) {
            dynamicValue.evalutationResult = getEvaluationResult(dynamicValue.expression, bindings, dynamicValue.hasProtectedAccess());
        } else {
            // A null result means that the value requires no interpolation and is to be returned as is
            dynamicValue.setInterpolationResult(interpolateIfRequired(dynamicValue, bindings));
        }
    }

    private EvaluationResult interpolateIfRequired(DynamicValue<?> dynamicValue, Map<String, Object> bindings) {
        Object value = dynamicValue.value;
        // Fast path: only string values may contain expressions or escape sequences, both of which start with a $
        if (!stringInterpolationEnabled || !(value instanceof String) || ((String) value).indexOf('$') < 0) {
            return null;
        }
        EvaluationResult result = new EvaluationResult();
        try {
            InterpolatedString interpolatedString = InterpolatedString.parse((String) value);
            if (interpolatedString.isVerbatim()) {
                return null;
            }
            interpolate(interpolatedString, bindings, dynamicValue.hasProtectedAccess(), result);
        } catch (Exception e) {
            result.setEvaluationException(e);
        }
        return result;
    }

    /**
     * Evaluates the expressions of the provided parsed string and concatenates them with its literal segments.
     * The literal segments are never passed to the expression handler, which is what makes the interpolation
     * safe with respect to quotes, backslashes and line breaks contained in the value.
     */
    private void interpolate(InterpolatedString interpolatedString, Map<String, Object> bindings, boolean hasProtectedAccess, EvaluationResult result) {
        StringBuilder clearValue = new StringBuilder();
        StringBuilder obfuscatedValue = new StringBuilder();
        boolean containsProtectedValues = false;
        for (InterpolatedString.Segment segment : interpolatedString.getSegments()) {
            if (segment.isExpression()) {
                Object o = expressionHandler.evaluateGroovyExpression(segment.getText(), bindings, hasProtectedAccess);
                if (o instanceof ProtectedVariable) {
                    // The clear value of a protected variable is only rendered into the result returned to
                    // callers with protected access. It is never rendered into the obfuscated result
                    ProtectedVariable protectedVariable = (ProtectedVariable) o;
                    obfuscatedValue.append(protectedVariable.obfuscatedValue);
                    clearValue.append(hasProtectedAccess ? String.valueOf(protectedVariable.value) : protectedVariable.obfuscatedValue);
                    containsProtectedValues = true;
                } else {
                    // Covers GString results as well, whose toString() renders the interpolated value
                    String stringValue = String.valueOf(o);
                    clearValue.append(stringValue);
                    obfuscatedValue.append(stringValue);
                }
            } else {
                clearValue.append(segment.getText());
                obfuscatedValue.append(segment.getText());
            }
        }
        result.setResultValue(containsProtectedValues ? obfuscatedValue.toString() : clearValue.toString());
        result.setProtectedValue(clearValue.toString());
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
