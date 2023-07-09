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

public class LessThanOperatorHandler extends AbstractOperatorHandler {
    @Override
    public boolean isSupported(Object value) {
        return isNumber(value);
    }

    @Override
    public AssertResult apply(String key, Object actual, String expectedValueString, boolean negate) {
        AssertResult assertResult = new AssertResult();
        assertResult.setPassed(negate ^ compare(actual, expectedValueString));
        assertResult.setMessage("'" + key + "' expected" + not(negate) + "to be less than '" + expectedValueString + "' " + (assertResult.isPassed() ? "and" : "but") + " was '" + actual + "'");
        assertResult.setDescription(key + not(negate) + "is less than '" + expectedValueString + "'");
        return assertResult;
    }

    private boolean compare(Object actual, String expectedValueString) {
        return compareNumeric(actual, expectedValueString) < 0;
    }
}
