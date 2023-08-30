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
package step.artefacts.handlers.asserts;

import java.math.BigDecimal;

public abstract class AbstractOperatorHandler implements AssertOperatorHandler {

    @Override
    public boolean isExpectedValueSupported(Object expectedValue) {
        // by default the expected value have the same restrictions as the value
        return isActualValueSupported(expectedValue);
    }

    protected String not(boolean negate) {
        return negate ? " not " : " ";
    }

    protected boolean isNumber(Object value) {
        return value instanceof Number;
    }

    protected boolean isBoolean(Object value) {
        return value instanceof Boolean;
    }

    protected boolean isString(Object value) {
        return value instanceof String;
    }

    protected boolean isNull(Object value) {
        return value == null;
    }

    protected boolean isAnyNumeric(Object value){
        return value instanceof Integer || value instanceof Long || value instanceof Double || value instanceof  BigDecimal || value instanceof Float;
    }

    protected int compareNumeric(Object actual, Object expectedValue) {
        // use big decimal to compare, for instance, Integer with Float
        return new BigDecimal(actual.toString()).compareTo(new BigDecimal(expectedValue.toString()));
    }

    /**
     * Resolve the target class to which both actual and expected values can be converted
     */
    protected Class<?> resolveTargetClass(Object actual, Object expectedValue) {
        // all numeric values we convert to BigDecimal
        if (actual.getClass().equals(expectedValue.getClass())) {
            return actual.getClass();
        } else if (expectedValue instanceof String) {
            return isAnyNumeric(actual) ? BigDecimal.class : actual.getClass();
        } else if (actual instanceof String) {
            return isAnyNumeric(expectedValue) ? BigDecimal.class : expectedValue.getClass();
        } else {
            throw new IllegalArgumentException("Non-convertible types: " + actual.getClass().getSimpleName() + " and " + expectedValue.getClass().getSimpleName());
        }
    }

    protected Object convert(Object value, Class<?> convertTo) {
        if (value.getClass().isAssignableFrom(convertTo)) {
            return value;
        } else if (BigDecimal.class.isAssignableFrom(convertTo)) {
            return new BigDecimal(value.toString());
        } else if (Boolean.class.isAssignableFrom(convertTo)) {
            return Boolean.parseBoolean(value.toString());
        } else if (String.class.isAssignableFrom(convertTo)) {
            return value.toString();
        } else {
            throw new IllegalArgumentException(value.getClass().getSimpleName() + " is not convertible to " + convertTo);
        }
    }

}
