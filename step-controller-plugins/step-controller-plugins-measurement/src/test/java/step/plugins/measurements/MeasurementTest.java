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
package step.plugins.measurements;

import org.junit.Test;
import step.core.reports.MetricSampleType;

import static org.junit.Assert.*;

public class MeasurementTest {

    /**
     * Verifies that setMetricType stores the string value (not the enum object) so that
     * MongoDB serialization produces a plain string field, compatible with existing documents.
     */
    @Test
    public void testMetricTypeStoredAsString() {
        Measurement m = new Measurement();
        m.setMetricType(MetricSampleType.RESPONSE_TIME.value());
        assertEquals("response-time", m.get(MeasurementPlugin.METRIC_TYPE_KEY));

        m.setMetricType(MetricSampleType.GAUGE.value());
        assertEquals("gauge", m.get(MeasurementPlugin.METRIC_TYPE_KEY));

        m.setMetricType(MetricSampleType.COUNTER.value());
        assertEquals("counter", m.get(MeasurementPlugin.METRIC_TYPE_KEY));
    }

    /**
     * Verifies that getMetricType correctly deserializes the stored string back to the enum
     * for all known values (roundtrip).
     */
    @Test
    public void testMetricTypeRoundtrip() {
        for (MetricSampleType type : MetricSampleType.values()) {
            Measurement m = new Measurement();
            m.setMetricType(type.value());
            assertEquals(type, m.getMetricType());
        }
    }

    /**
     * Verifies that getMetricType can deserialize string values that were stored by older
     * code directly using the string constant (e.g. MeasurementPlugin.METRIC_TYPE_RESPONSE_TIME).
     * This ensures backward compatibility with existing documents in the database.
     */
    @Test
    public void testMetricTypeDeserializationFromLegacyStringValue() {
        Measurement m = new Measurement();
        m.put(MeasurementPlugin.METRIC_TYPE_KEY, "response-time");
        assertEquals(MetricSampleType.RESPONSE_TIME, m.getMetricType());
    }

    /**
     * Verifies that getMetricType returns null when the field is absent,
     * matching measurements created before the metricType field was introduced.
     */
    @Test
    public void testGetMetricTypeNullWhenAbsent() {
        Measurement m = new Measurement();
        assertNull(m.getMetricType());
    }
}
