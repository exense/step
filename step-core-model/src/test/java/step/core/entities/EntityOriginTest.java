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
package step.core.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The (de)serializers are format agnostic, so JSON is used here. The YAML level is covered by the plan reader tests.
 */
public class EntityOriginTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void plainFormIsReadAsString() throws Exception {
        EntityOrigin origin = mapper.readValue("\"ai\"", EntityOrigin.class);

        assertEquals("ai", origin.getBy());
        assertNull(origin.getModel());
        assertNull(origin.getTimestamp());
        assertTrue(origin.isAi());
        assertTrue(origin.isPlainForm());
    }

    @Test
    public void plainFormIsWrittenAsString() throws Exception {
        assertEquals("\"ai\"", mapper.writeValueAsString(EntityOrigin.ai()));
    }

    @Test
    public void plainFormRoundTrips() throws Exception {
        EntityOrigin origin = EntityOrigin.ai();

        assertEquals(origin, mapper.readValue(mapper.writeValueAsString(origin), EntityOrigin.class));
    }

    /**
     * The object form must already be readable today, otherwise extending the marker later would be a breaking
     * schema change.
     */
    @Test
    public void objectFormIsReadAsObject() throws Exception {
        EntityOrigin origin = mapper.readValue(
                "{\"by\":\"ai\",\"model\":\"some-model\",\"timestamp\":\"2026-07-30T12:00:00Z\"}", EntityOrigin.class);

        assertEquals("ai", origin.getBy());
        assertEquals("some-model", origin.getModel());
        assertEquals("2026-07-30T12:00:00Z", origin.getTimestamp());
        assertTrue(origin.isAi());
        assertFalse(origin.isPlainForm());
    }

    @Test
    public void objectFormRoundTripsAsObject() throws Exception {
        EntityOrigin origin = EntityOrigin.ai();
        origin.setModel("some-model");

        String serialized = mapper.writeValueAsString(origin);

        assertEquals("{\"by\":\"ai\",\"model\":\"some-model\"}", serialized);
        assertEquals(origin, mapper.readValue(serialized, EntityOrigin.class));
    }

    @Test
    public void nullIsReadAsNull() throws Exception {
        assertNull(mapper.readValue("null", EntityOrigin.class));
    }

    @Test
    public void emptyOriginIsWrittenAsNull() throws Exception {
        assertEquals("null", mapper.writeValueAsString(new EntityOrigin()));
    }

    @Test
    public void unknownOriginIsNotAi() throws Exception {
        assertFalse(mapper.readValue("\"human\"", EntityOrigin.class).isAi());
    }
}
