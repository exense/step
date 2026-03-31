package step.plugins.timeseries;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.metrics.MetricSample;
import step.core.metrics.MetricType;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.TimeSeriesBuilder;
import step.core.timeseries.TimeSeriesCollection;
import step.core.timeseries.bucket.Bucket;
import step.plugins.measurements.StepMetricSample;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unit tests for {@link TimeSeriesBucketingHandler#processMetrics}.
 */
public class TimeSeriesBucketingHandlerTest {

    private static final int BUCKET_RESOLUTION = 1000;
    private static final Set<String> HANDLED_ATTRIBUTES = Set.of("eId", "name", "env");

    private InMemoryCollection<Bucket> bucketsCollection;
    private TimeSeriesBucketingHandler handler;

    @Before
    public void setUp() {
        bucketsCollection = new InMemoryCollection<>();
        TimeSeriesCollection tsCollection = new TimeSeriesCollection(bucketsCollection, BUCKET_RESOLUTION);
        TimeSeries timeSeries = new TimeSeriesBuilder()
            .registerCollection(tsCollection)
            .build();
        handler = new TimeSeriesBucketingHandler(timeSeries, HANDLED_ATTRIBUTES, Set.of());
    }

    // ── Counter ──────────────────────────────────────────────────────────────

    @Test
    public void counter_ingestsAccumulatedDiffAsPoint() {
        MetricSample snapshot = new MetricSample(0L,
            "requests", Map.of("env", "prod"), MetricType.COUNTER, 7, 42, 42, 42, 42, null);

        StepMetricSample mm = buildMetricMeasurement(snapshot);

        handler.processMetrics(List.of(mm));
        handler.flush();

        List<Bucket> buckets = allBuckets();
        Assert.assertEquals(1, buckets.size());
        Bucket b = buckets.get(0);
        // count = accumulatedDiff → use for rate (count/duration)
        Assert.assertEquals(7, b.getCount());
        // sum = min = max = longRunningTotal → use for absolute total display (LAST/MAX, not SUM)
        Assert.assertEquals(42, b.getSum());
        Assert.assertEquals(42, b.getMin());
        Assert.assertEquals(42, b.getMax());
        Assert.assertEquals("counter", b.getAttributes().get("metricType"));
        Assert.assertEquals("requests", b.getAttributes().get("name"));
        Assert.assertEquals("prod", b.getAttributes().get("env"));
    }


    // ── Gauge ─────────────────────────────────────────────────────────────────

    @Test
    public void gauge_ingestsFullBucket() {
        MetricSample snapshot = new MetricSample(1000L,
            "queue_depth", Map.of("env", "staging"), MetricType.GAUGE,
            3, 57, 15, 42, 42, null);
        StepMetricSample mm = buildMetricMeasurement(snapshot);

        handler.processMetrics(List.of(mm));
        handler.flush();

        List<Bucket> buckets = allBuckets();
        Assert.assertEquals(1, buckets.size());
        Bucket b = buckets.get(0);
        Assert.assertEquals(3, b.getCount());
        Assert.assertEquals(57, b.getSum());
        Assert.assertEquals(15, b.getMin());
        Assert.assertEquals(42, b.getMax());
        Assert.assertEquals("gauge", b.getAttributes().get("metricType"));
        Assert.assertEquals("queue_depth", b.getAttributes().get("name"));
        Assert.assertEquals("staging", b.getAttributes().get("env"));
    }

    // ── Histogram ────────────────────────────────────────────────────────────

    @Test
    public void histogram_ingestsFullBucketWithDistribution() {
        Map<Long, Long> dist = Map.of(100L, 1L, 200L, 1L);
        MetricSample snapshot = new MetricSample(2000L,
            "response_time_ms", Map.of(), MetricType.HISTOGRAM,
            2, 300, 100, 200, 200, dist);
        StepMetricSample mm = buildMetricMeasurement(snapshot);

        handler.processMetrics(List.of(mm));
        handler.flush();

        List<Bucket> buckets = allBuckets();
        Assert.assertEquals(1, buckets.size());
        Bucket b = buckets.get(0);
        Assert.assertEquals(2, b.getCount());
        Assert.assertEquals(300, b.getSum());
        Assert.assertEquals(100, b.getMin());
        Assert.assertEquals(200, b.getMax());
        Assert.assertEquals("histogram", b.getAttributes().get("metricType"));
        Assert.assertEquals("response_time_ms", b.getAttributes().get("name"));
        Assert.assertNotNull(b.getDistribution());
        Assert.assertEquals(dist, b.getDistribution());
    }

    // ── Attribute filtering ───────────────────────────────────────────────────

    @Test
    public void onlyHandledAttributesAreIncludedInBucket() {
        // "region" is not in HANDLED_ATTRIBUTES, so it must be absent from the bucket
        MetricSample snapshot = new MetricSample(0L,
            "cpu", Map.of("env", "qa", "region", "us-east"), MetricType.GAUGE,
            1, 80, 80, 80, 80, null);
        StepMetricSample mm = buildMetricMeasurement(snapshot);

        handler.processMetrics(List.of(mm));
        handler.flush();

        List<Bucket> buckets = allBuckets();
        Assert.assertEquals(1, buckets.size());
        Assert.assertFalse(buckets.get(0).getAttributes().containsKey("region"));
        Assert.assertEquals("qa", buckets.get(0).getAttributes().get("env"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private StepMetricSample buildMetricMeasurement(MetricSample snapshot) {
        return new StepMetricSample(
            snapshot,
            "exec-1",        // execId
            "rn-1",          // rnId
            "plan-1",        // planId
            "MyPlan",        // plan name
            "",              // taskId
            "",              // schedule
            "",              // execution description
            null,            // agentUrl
            null,            // origin
            null             // additionalAttributes
        );
    }

    private List<Bucket> allBuckets() {
        return bucketsCollection.find(
            Filters.empty(), null, null, null, 0
        ).collect(Collectors.toList());
    }
}
