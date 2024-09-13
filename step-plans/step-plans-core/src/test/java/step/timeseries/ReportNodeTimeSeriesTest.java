package step.timeseries;

import ch.exense.commons.app.Configuration;
import org.junit.Assert;
import org.junit.Test;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.artefacts.reports.aggregated.ReportNodeTimeSeries;
import step.core.collections.CollectionFactory;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.timeseries.TimeSeries;

public class ReportNodeTimeSeriesTest {

    @Test
    public void reportNodeTimeSeries() {
        Configuration configuration = new Configuration();
        configuration.putProperty(ReportNodeTimeSeries.TIME_SERIES_MINUTE_COLLECTION_ENABLED, "true");
        configuration.putProperty(ReportNodeTimeSeries.TIME_SERIES_HOUR_COLLECTION_ENABLED, "true");
        configuration.putProperty(ReportNodeTimeSeries.TIME_SERIES_DAY_COLLECTION_ENABLED, "true");
        configuration.putProperty(ReportNodeTimeSeries.TIME_SERIES_WEEK_COLLECTION_ENABLED, "true");
        CollectionFactory collectionFactory = new InMemoryCollectionFactory(null);
        ReportNodeTimeSeries reportNodeTimeSeries = new ReportNodeTimeSeries(collectionFactory, configuration);
        TimeSeries timeSeries = reportNodeTimeSeries.getTimeSeries();
        Assert.assertEquals(5, timeSeries.getCollections().size());
        ReportNode reportNode = new ReportNode();
        reportNode.setStatus(ReportNodeStatus.PASSED);
        reportNode.setExecutionID("executionId");
        reportNode.setArtefactHash("artefactHash");
        reportNode.setExecutionTime(1000);

        reportNodeTimeSeries.ingestReportNode(reportNode);
        // manuall flush all collections
        reportNodeTimeSeries.getTimeSeries().getCollections().forEach(c -> c.getIngestionPipeline().flush());
        reportNodeTimeSeries.getTimeSeries().getCollections().forEach(c -> {
            long count = c.getCollection().count(Filters.empty(), null);
            Assert.assertEquals(1, count);
        });
    }

}
