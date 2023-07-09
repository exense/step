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

    protected int compareNumeric(Object actual, String expectedValueString) {
        if (actual instanceof Integer) {
            return ((Integer) actual).compareTo(Integer.valueOf(expectedValueString));
        } else if (actual instanceof Long) {
            return ((Long) actual).compareTo(Long.valueOf(expectedValueString));
        } else if (actual instanceof Double) {
            return ((Double) actual).compareTo(Double.valueOf(expectedValueString));
        } else if(actual instanceof BigDecimal){
            return ((BigDecimal) actual).compareTo(BigDecimal.valueOf(Double.parseDouble(expectedValueString)));
        } else if(actual instanceof Float){
            return ((Float) actual).compareTo(Float.valueOf(expectedValueString));
        } else {
            throw new IllegalArgumentException("Not supported value: " + actual.getClass().getName());
        }
    }
}
