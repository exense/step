/*
 * Copyright (C) 2024, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

package step.core.timeseries;

import ch.exense.commons.app.Configuration;
import org.junit.Test;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.artefacts.reports.aggregated.ReportNodeTimeSeries;
import step.core.collections.CollectionFactory;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ReportNodeTimeSeriesTest {

    @Test
    public void reportNodeTimeSeries() {
        Configuration configuration = new Configuration();
        configuration.putProperty("reportNodeTimeSeries.collections.minute.enabled", "true");
        configuration.putProperty("reportNodeTimeSeries.collections.hour.enabled", "true");
        configuration.putProperty("reportNodeTimeSeries.collections.day.enabled", "true");
        configuration.putProperty("reportNodeTimeSeries.collections.week.enabled", "true");
        CollectionFactory collectionFactory = new InMemoryCollectionFactory(null);
        try (ReportNodeTimeSeries reportNodeTimeSeries = new ReportNodeTimeSeries(collectionFactory, configuration)) {
            TimeSeries timeSeries = reportNodeTimeSeries.getTimeSeries();
            List<TimeSeriesCollection> collections = timeSeries.getCollections();
            assertEquals(5, collections.size());
            ReportNode reportNode = new ReportNode();
            reportNode.setStatus(ReportNodeStatus.PASSED);
            reportNode.setExecutionID("executionId");
            reportNode.setArtefactHash("artefactHash");
            reportNode.setExecutionTime(1000);
            reportNode.setDuration(1000);

            reportNodeTimeSeries.ingestReportNode(reportNode);
            // manually flush all collections
            collections.forEach(c -> c.getIngestionPipeline().flush());
            collections.forEach(c -> {
                long count = c.count(Filters.empty(), null);
                assertEquals(1, count);
            });
        }
    }

    @Test
    public void reportNodeTimeSeriesDisabled() {
        Configuration configuration = new Configuration();
        configuration.putProperty("reportNodeTimeSeries.collections.minute.enabled", "false");
        configuration.putProperty("reportNodeTimeSeries.collections.hour.enabled", "false");
        configuration.putProperty("reportNodeTimeSeries.collections.day.enabled", "false");
        configuration.putProperty("reportNodeTimeSeries.collections.week.enabled", "false");
        CollectionFactory collectionFactory = new InMemoryCollectionFactory(null);
        ReportNodeTimeSeries reportNodeTimeSeries = new ReportNodeTimeSeries(collectionFactory, configuration);
        TimeSeries timeSeries = reportNodeTimeSeries.getTimeSeries();
        List<TimeSeriesCollection> collections = timeSeries.getCollections();
        assertEquals(1, collections.size());
    }

    @Test
    public void reportNodeTimeSeriesFlushPeriods() {
        Configuration configuration = new Configuration();
        configuration.putProperty("reportNodeTimeSeries.collections.minute.flush.period", "11");
        configuration.putProperty("reportNodeTimeSeries.collections.hour.flush.period", "12");
        configuration.putProperty("reportNodeTimeSeries.collections.day.flush.period", "13");
        configuration.putProperty("reportNodeTimeSeries.collections.week.flush.period", "14");

        TimeSeriesCollectionsSettings settings = TimeSeriesCollectionsSettings.readSettings(configuration, "reportNodeTimeSeries");
        assertEquals(11, settings.getPerMinuteFlushInterval());
        assertEquals(12, settings.getHourlyFlushInterval());
        assertEquals(13, settings.getDailyFlushInterval());
        assertEquals(14, settings.getWeeklyFlushInterval());
    }

}
