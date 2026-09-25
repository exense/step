/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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

import org.junit.Assert;
import org.junit.Test;

public class DynamicValueEqualityTest {

    /**
     * A value holding null has no string form, and an unset field of a yaml model is exactly that -
     * which is what a serializer comparing a value against the default of its field runs into.
     */
    @Test
    public void testValuesHoldingNullAreEqual() {
        Assert.assertEquals(new DynamicValue<String>(), new DynamicValue<String>(null));
        Assert.assertEquals(new DynamicValue<String>().hashCode(), new DynamicValue<String>(null).hashCode());
        Assert.assertNotEquals(new DynamicValue<String>(), new DynamicValue<>("script.js"));
    }

    @Test
    public void testLiteralValuesAreComparedByWhatTheyHold() {
        Assert.assertEquals(new DynamicValue<>("script.js"), new DynamicValue<>("script.js"));
        Assert.assertEquals(new DynamicValue<>("script.js").hashCode(), new DynamicValue<>("script.js").hashCode());
        Assert.assertNotEquals(new DynamicValue<>(180000), new DynamicValue<>("180000"));
    }

    /**
     * An expression which hasn't been evaluated has no result, so it is compared as the expression it is
     */
    @Test
    public void testExpressionsAreComparedUnevaluated() {
        DynamicValue<String> expression = new DynamicValue<>("'script.js'", "groovy");

        Assert.assertEquals(expression, new DynamicValue<String>("'script.js'", "groovy"));
        Assert.assertNotEquals(expression, new DynamicValue<>("script.js"));
        Assert.assertNotEquals(expression, new DynamicValue<String>("'script.js'", "javascript"));
    }
}
