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

import org.junit.Assert;
import org.junit.Test;

public class EqualsOperatorHandlerTest {

    @Test
    public void testNumericEquals(){
        // we support comparison between strings and numbers

        EqualsOperatorHandler handler = new EqualsOperatorHandler();
        AssertResult result;

        // compare integers
        result = handler.apply("testKey", 778, "778", false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", "778", 778, false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", "779", 778, false);
        Assert.assertFalse(result.isPassed());

        // compare double with strings
        result = handler.apply("testKey", 778.0, "778", false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", 778.0, "778.00", false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", "778.0", 778.00, false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", "778.01", 778.00, false);
        Assert.assertFalse(result.isPassed());

        result = handler.apply("testKey", 778.0, "778.01", false);
        Assert.assertFalse(result.isPassed());

        // compare double with double
        result = handler.apply("testKey", 778.0, 778.00, false);
        Assert.assertTrue(result.isPassed());
    }
}
