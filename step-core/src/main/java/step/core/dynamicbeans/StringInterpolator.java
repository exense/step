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

import step.expressions.ExpressionHandler;
import step.expressions.ProtectedVariable;

/**
 * Interpolates the expressions contained in plain (non dynamic) string values. See {@link InterpolatedString}
 * for the supported syntax.
 * <p>
 * The literal segments of the value are never passed to the expression handler, only the expressions they
 * delimit are. This is what makes the interpolation safe with respect to the quotes, backslashes and line
 * breaks contained in the value, which the groovy lexer would otherwise interpret.
 */
public class StringInterpolator {

    /**
     * The result of the interpolation of a string value, in its clear and obfuscated forms
     */
    public static class Result {

        private final String value;
        private final String obfuscatedValue;
        private final boolean containsProtectedValues;

        private Result(String value, String obfuscatedValue, boolean containsProtectedValues) {
            this.value = value;
            this.obfuscatedValue = obfuscatedValue;
            this.containsProtectedValues = containsProtectedValues;
        }

        /**
         * @return the interpolated value. It only contains the clear value of the protected variables used by
         * the expressions if protected access was granted
         */
        public String getValue() {
            return value;
        }

        /**
         * @return the interpolated value in which the protected variables used by the expressions are obfuscated
         */
        public String getObfuscatedValue() {
            return obfuscatedValue;
        }

        /**
         * @return true if any of the expressions resolved to a protected variable
         */
        public boolean containsProtectedValues() {
            return containsProtectedValues;
        }
    }

    private final ExpressionHandler expressionHandler;

    /**
     * @param expressionHandler the handler used to evaluate the expressions
     */
    public StringInterpolator(ExpressionHandler expressionHandler) {
        super();
        this.expressionHandler = expressionHandler;
    }

    /**
     * Interpolates the expressions contained in the provided value
     *
     * @param value              the value to be interpolated. Values which aren't strings are ignored
     * @param bindings           the set of bindings (variables) available for the evaluation of the expressions
     * @param hasProtectedAccess whether the expressions may access protected bindings
     * @return the result of the interpolation or null if the value requires no interpolation, which is the case
     * for the vast majority of the values
     * @throws StringInterpolationException if the value contains a malformed expression
     * @throws RuntimeException             if the evaluation of one of the expressions fails
     */
    public Result interpolate(Object value, Map<String, Object> bindings, boolean hasProtectedAccess) {
        // Fast path: only string values may contain expressions or escape sequences, both of which start with a $
        if (!(value instanceof String) || ((String) value).indexOf('$') < 0) {
            return null;
        }
        InterpolatedString interpolatedString = InterpolatedString.parse((String) value);
        if (interpolatedString.isVerbatim()) {
            return null;
        }
        return interpolate(interpolatedString, bindings, hasProtectedAccess);
    }

    private Result interpolate(InterpolatedString interpolatedString, Map<String, Object> bindings, boolean hasProtectedAccess) {
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
        return new Result(clearValue.toString(), obfuscatedValue.toString(), containsProtectedValues);
    }
}
