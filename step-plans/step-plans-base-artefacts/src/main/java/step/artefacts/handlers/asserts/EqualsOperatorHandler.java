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
import java.util.Objects;

public class EqualsOperatorHandler extends AbstractOperatorHandler {

    @Override
    public boolean isActualValueSupported(Object value) {
        return (isBoolean(value) || isString(value) || isNumber(value));
    }

    @Override
    public AssertResult apply(String key, Object actual, Object expectedValue, boolean negate) {
        AssertResult assertResult = new AssertResult();
        assertResult.setPassed(negate ^ checkEquals(actual, expectedValue));
        assertResult.setMessage("'" + key + "' expected" + not(negate) + "to be equal to '" + expectedValue + "' " + (assertResult.isPassed() ? "and" : "but") + " was '" + actual + "'");
        assertResult.setDescription(key + (negate ? " !" : " ") + "= '" + expectedValue + "'");
        return assertResult;
    }

    private boolean checkEquals(Object actual, Object expectedValue) {
        Class<?> targetClass = resolveTargetClass(actual, expectedValue);
        Object actualConvertedToTarget = convert(actual, targetClass);
        Object expectedConvertedToTarget = convert(expectedValue, targetClass);
        if (actualConvertedToTarget instanceof BigDecimal && expectedConvertedToTarget instanceof BigDecimal) {
            // compare instead of equals to avoid scaling mismatch in actual and expected values
            return ((BigDecimal) actualConvertedToTarget).compareTo((BigDecimal) expectedConvertedToTarget) == 0;
        } else {
            return Objects.equals(actualConvertedToTarget, expectedConvertedToTarget);
        }
    }

}
