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

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import step.expressions.ExpressionHandler;
import step.expressions.ProtectedVariable;

public class DynamicJsonValueResolver {

    private ExpressionHandler expressionHandler;

    private final StringInterpolator stringInterpolator;

    public DynamicJsonValueResolver(ExpressionHandler expressionHandler) {
        this(expressionHandler, new StringInterpolator(expressionHandler));
    }

    public DynamicJsonValueResolver(ExpressionHandler expressionHandler, StringInterpolator stringInterpolator) {
        super();
        this.expressionHandler = expressionHandler;
        this.stringInterpolator = stringInterpolator;
    }

    public Object evaluate(JsonObject dynamicValueAsJson, Map<String, Object> bindings, boolean canAccessProtectedValue) {
        boolean isDynamic = dynamicValueAsJson.getBoolean("dynamic");

        if (isDynamic) {
            String exprType = dynamicValueAsJson.containsKey("expressionType") ? dynamicValueAsJson.getString("expressionType") : null;
            String expression = dynamicValueAsJson.getString("expression");
            try {
                return expressionHandler.evaluateGroovyExpression(expression, bindings, canAccessProtectedValue);
            } catch (Exception e) {
                throw wrapEvaluationException(e);
            }
        } else {
            return interpolateIfRequired(dynamicValueAsJson.get("value"), bindings, canAccessProtectedValue);
        }
    }

    /**
     * Interpolates the expressions contained in a plain (non dynamic) value. Only string values are concerned,
     * any other value is returned as is
     */
    private Object interpolateIfRequired(JsonValue value, Map<String, Object> bindings, boolean canAccessProtectedValue) {
        if (!(value instanceof JsonString)) {
            return value;
        }
        try {
            StringInterpolator.Result interpolation = stringInterpolator.interpolate(((JsonString) value).getString(), bindings, canAccessProtectedValue);
            if (interpolation == null) {
                return value;
            }
            // Returning a ProtectedVariable lets the caller keep track of both the clear and obfuscated results
            return interpolation.containsProtectedValues() ?
                new ProtectedVariable(null, interpolation.getValue(), interpolation.getObfuscatedValue()) :
                interpolation.getValue();
        } catch (Exception e) {
            throw wrapEvaluationException(e);
        }
    }

    private static RuntimeException wrapEvaluationException(Exception e) {
        Throwable cause = e.getCause();
        String errorMsg = e.getMessage();
        if (cause != null) {
            errorMsg = errorMsg + ". Groovy error: >>> " + cause.getMessage() + " <<<";
        }
        return new RuntimeException(errorMsg, e);
    }

}
