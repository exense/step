package step.core.timeseries;

import ch.exense.commons.app.Configuration;
import org.junit.Test;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.artefacts.reports.aggregated.ReportNodeTimeSeries;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.timeseries.bucket.Bucket;
import step.core.timeseries.bucket.BucketBuilder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static step.core.artefacts.reports.aggregated.ReportNodeTimeSeries.*;

public class ReportNodeTimeSeriesCompensationTest {

    private static final String EXECUTION = "executionId";
    private static final String HASH = "artefactHash";
    private static final long TIMESTAMP = 60000;

    private ReportNodeTimeSeries newReportNodeTimeSeries() {
        Configuration configuration = new Configuration();
        configuration.putProperty("reportNodeTimeSeries.collections.minute.enabled", "true");
        configuration.putProperty("reportNodeTimeSeries.collections.hour.enabled", "true");
        configuration.putProperty("reportNodeTimeSeries.collections.day.enabled", "true");
        return new ReportNodeTimeSeries(new InMemoryCollectionFactory(null), configuration);
    }

    private ReportNode reportNode(ReportNodeStatus status, long timestamp, int duration) {
        ReportNode reportNode = new ReportNode();
        reportNode.setStatus(status);
        reportNode.setExecutionID(EXECUTION);
        reportNode.setArtefactHash(HASH);
        reportNode.setExecutionTime(timestamp);
        reportNode.setDuration(duration);
        return reportNode;
    }

    private Map<String, Long> countByStatus(ReportNodeTimeSeries reportNodeTimeSeries) {
        return reportNodeTimeSeries.queryByExecutionIdAndGroupBy(EXECUTION, null, ARTEFACT_HASH, STATUS)
            .values().stream().flatMap(byStatus -> byStatus.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getCount(), Long::sum));
    }

    private void flush(ReportNodeTimeSeries reportNodeTimeSeries) {
        reportNodeTimeSeries.getTimeSeries().getCollections().forEach(c -> c.getIngestionPipeline().flush());
    }

    private List<Bucket> buckets(TimeSeriesCollection collection) {
        return collection.find(Filters.empty()).collect(Collectors.toList());
    }

    @Test
    public void compensatedNodeStopsCounting() {
        try (ReportNodeTimeSeries reportNodeTimeSeries = newReportNodeTimeSeries()) {
            ReportNode failed = reportNode(ReportNodeStatus.FAILED, TIMESTAMP, 100);
            reportNodeTimeSeries.ingestReportNode(failed, Map.of());
            reportNodeTimeSeries.ingestReportNode(reportNode(ReportNodeStatus.PASSED, TIMESTAMP, 20), Map.of());

            reportNodeTimeSeries.compensateReportNode(failed, Map.of());
            flush(reportNodeTimeSeries);

            // The compensated samples are gone at every resolution, the other ones are untouched
            assertEquals(Map.of(ReportNodeStatus.FAILED.name(), 0L, ReportNodeStatus.PASSED.name(), 1L),
                countByStatus(reportNodeTimeSeries));
            reportNodeTimeSeries.getTimeSeries().getCollections().forEach(collection -> {
                Bucket failedBucket = buckets(collection).stream()
                    .filter(b -> ReportNodeStatus.FAILED.name().equals(b.getAttributes().get(STATUS)))
                    .reduce((a, b) -> {
                        throw new IllegalStateException("More than one bucket per series and window");
                    }).orElseThrow();
                assertEquals("Collection " + collection.getName(), 0, failedBucket.getCount());
                assertEquals("Collection " + collection.getName(), 0, failedBucket.getSum());
                // The distribution is subtracted bracket by bracket: the bracket of the compensated sample is emptied
                assertEquals("Collection " + collection.getName(), Map.of(100L, 0L), failedBucket.getDistribution());
            });
        }
    }

    @Test
    public void compensationSubtractsTheDistributionOfTheCompensatedSamples() {
        try (ReportNodeTimeSeries reportNodeTimeSeries = newReportNodeTimeSeries()) {
            // Two samples in the same series, one of them compensated. Durations 100 and 230 fall into two different
            // brackets of the distribution, the bracket precision being 10
            ReportNode kept = reportNode(ReportNodeStatus.FAILED, TIMESTAMP, 230);
            ReportNode compensated = reportNode(ReportNodeStatus.FAILED, TIMESTAMP, 100);
            reportNodeTimeSeries.ingestReportNode(kept, Map.of());
            reportNodeTimeSeries.ingestReportNode(compensated, Map.of());
            flush(reportNodeTimeSeries);

            reportNodeTimeSeries.compensateReportNode(compensated, Map.of());
            flush(reportNodeTimeSeries);

            // Only the bracket of the compensated sample is emptied, and the percentiles are the ones of the sample
            // left, not of both samples
            Bucket bucket = mergedBucket(reportNodeTimeSeries.getTimeSeries().getDefaultCollection());
            assertEquals(1, bucket.getCount());
            assertEquals(230, bucket.getSum());
            assertEquals(Map.of(100L, 0L, 230L, 1L), bucket.getDistribution());
            assertEquals(230, bucket.getPercentile(50));
            assertEquals(230, bucket.getPercentile(90));
        }
    }

    private Bucket mergedBucket(TimeSeriesCollection collection) {
        List<Bucket> buckets = buckets(collection);
        BucketBuilder bucketBuilder = BucketBuilder.create(buckets.get(0).getBegin());
        buckets.forEach(bucketBuilder::merge);
        return bucketBuilder.build();
    }

    @Test
    public void compensationIsExactOnceTheDataIsSaved() {
        try (ReportNodeTimeSeries reportNodeTimeSeries = newReportNodeTimeSeries()) {
            ReportNode failed = reportNode(ReportNodeStatus.FAILED, TIMESTAMP, 100);
            reportNodeTimeSeries.ingestReportNode(failed, Map.of());
            // The data is saved before the node is removed, as for a retry with a grace period
            flush(reportNodeTimeSeries);

            reportNodeTimeSeries.compensateReportNode(failed, Map.of());
            flush(reportNodeTimeSeries);

            assertEquals(Map.of(ReportNodeStatus.FAILED.name(), 0L), countByStatus(reportNodeTimeSeries));
            // The compensation is saved as an additional bucket of the same series and window
            TimeSeriesCollection collection = reportNodeTimeSeries.getTimeSeries().getDefaultCollection();
            List<Bucket> buckets = buckets(collection);
            assertEquals(2, buckets.size());
            long expectedWindow = TIMESTAMP - TIMESTAMP % collection.getResolutionMs();
            buckets.forEach(b -> assertEquals(expectedWindow, b.getBegin()));
        }
    }

    @Test
    public void compensationKeepsTheCustomAttributes() {
        try (ReportNodeTimeSeries reportNodeTimeSeries = newReportNodeTimeSeries()) {
            Map<String, Object> customAttributes = Map.of("type", "CheckArtefact", "name", "myKeyword");
            ReportNode failed = reportNode(ReportNodeStatus.FAILED, TIMESTAMP, 100);
            failed.setError("myError", 1, true);
            reportNodeTimeSeries.ingestReportNode(failed, customAttributes);
            flush(reportNodeTimeSeries);

            reportNodeTimeSeries.compensateReportNode(failed, customAttributes);
            flush(reportNodeTimeSeries);

            // Both buckets belong to the same series: the error and the custom attributes are part of it
            TimeSeriesCollection collection = reportNodeTimeSeries.getTimeSeries().getDefaultCollection();
            List<Bucket> buckets = buckets(collection);
            assertEquals(2, buckets.size());
            assertEquals(buckets.get(0).getAttributes(), buckets.get(1).getAttributes());
            assertEquals(0, buckets.stream().mapToLong(Bucket::getCount).sum());
            assertEquals(Map.of(), countByErrorMessage(reportNodeTimeSeries));
        }
    }

    private Map<String, Long> countByErrorMessage(ReportNodeTimeSeries reportNodeTimeSeries) {
        return reportNodeTimeSeries.queryByExecutionIdAndGroupBy(EXECUTION, null, ARTEFACT_HASH, ERROR_MESSAGE)
            .values().stream().flatMap(byMessage -> byMessage.entrySet().stream())
            .filter(e -> e.getValue().getCount() > 0)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getCount(), Long::sum));
    }
}
