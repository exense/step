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
package step.plans.parser.yaml.model;

import org.junit.Test;
import step.core.dynamicbeans.DynamicValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The two forms yaml has for the file of a data source - a path, or the id of a Step resource - and
 * how each reference an entity may carry maps onto them.
 */
public class YamlResourceReferenceTest {

    private static YamlResourceReference of(String reference) {
        return YamlResourceReference.fromDynamicValue(new DynamicValue<>(reference));
    }

    @Test
    public void aStepResourceIsWrittenAsAnId() {
        YamlResourceReference reference = of("resource:644fbe4e38a61e07cc3a4df9");

        assertEquals("644fbe4e38a61e07cc3a4df9", reference.getResourceId());
        assertNull(reference.getSimpleString());
    }

    /**
     * The archive-relative path is what the descriptor was authored with, and what it gets back. The
     * {@code apId} names the package the entity was read from, which no descriptor holds: it is put
     * back by {@code AutomationPackageResourceMapper} the next time the package is read.
     */
    @Test
    public void aResourceOfADeployedAutomationPackageIsWrittenAsItsPath() {
        YamlResourceReference reference = of("apResource:644fbe4e38a61e07cc3a4df9:data/my book.xlsx");

        assertEquals("data/my book.xlsx", reference.getSimpleString());
        assertNull(reference.getResourceId());
    }

    /**
     * The editor form maps the same way - the whole point being that it no longer needs a mapping of
     * its own, applied by whoever happens to be writing the package.
     */
    @Test
    public void aResourceOfTheAutomationPackageBeingEditedIsWrittenAsItsPath() {
        YamlResourceReference reference = of("apResource:local:data/my book.xlsx");

        assertEquals("data/my book.xlsx", reference.getSimpleString());
        assertNull(reference.getResourceId());
    }

    /**
     * A path holding a colon of its own must not be read as the separator of the reference.
     */
    @Test
    public void aPathHoldingASeparatorSurvives() {
        assertEquals("data/2024:Q1/pool.xlsx", of("apResource:local:data/2024:Q1/pool.xlsx").getSimpleString());
    }

    @Test
    public void aPathIsWrittenAsItIs() {
        assertEquals("data/my book.xlsx", of("data/my book.xlsx").getSimpleString());
        assertEquals("C:\\books\\my book.xlsx", of("C:\\books\\my book.xlsx").getSimpleString());
    }

    @Test
    public void anAbsentReferenceIsEmpty() {
        assertTrue(of(null).isEmpty());
    }

    /**
     * The round trip a descriptor makes: what is written as a path is read back as one, and
     * {@code AutomationPackageResourceMapper} turns it into the reference of the package being read.
     */
    @Test
    public void aPathIsReadBackAsItWasWritten() {
        assertEquals("data/my book.xlsx",
            of("apResource:local:data/my book.xlsx").toDynamicValue().getValue());
        assertEquals("resource:644fbe4e38a61e07cc3a4df9",
            of("resource:644fbe4e38a61e07cc3a4df9").toDynamicValue().getValue());
    }
}
