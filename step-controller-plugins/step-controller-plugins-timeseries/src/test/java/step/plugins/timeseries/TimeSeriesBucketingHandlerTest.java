package step.plugins.timeseries;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.metrics.Measurement;
import step.core.metrics.MetricSample;
import step.core.metrics.MetricsExecutionPlugin;
import step.core.metrics.InstrumentType;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.TimeSeriesBuilder;
import step.core.timeseries.TimeSeriesCollection;
import step.core.timeseries.bucket.Bucket;
import step.core.metrics.ExecutionMetricSample;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unit tests for {@link TimeSeriesMetricSamplesHandler#processMetrics}.
 */
public class TimeSeriesBucketingHandlerTest {

    private static final int BUCKET_RESOLUTION = 1000;
    private static final Set<String> HANDLED_ATTRIBUTES = Set.of("eId", "name", "env");

    private InMemoryCollection<Bucket> bucketsCollection;
    private TimeSeriesMetricSamplesHandler handler;

    @Before
    public void setUp() {
        bucketsCollection = new InMemoryCollection<>();
        TimeSeriesCollection tsCollection = new TimeSeriesCollection(bucketsCollection, BUCKET_RESOLUTION);
        TimeSeries timeSeries = new TimeSeriesBuilder()
            .registerCollection(tsCollection)
            .build();
        handler = new TimeSeriesMetricSamplesHandler(timeSeries, HANDLED_ATTRIBUTES, Set.of());
    }

    // ── Counter ──────────────────────────────────────────────────────────────

    @Test
    public void counter_ingestsAccumulatedDiffAsPoint() {
        MetricSample snapshot = new MetricSample(0L,
            "requests", Map.of("env", "prod"), InstrumentType.COUNTER, 7, 42, 42, 42, 42, null);

        ExecutionMetricSample mm = buildMetricMeasurement(snapshot);

        handler.processMetrics(null, List.of(mm));
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
            "queue_depth", Map.of("env", "staging"), InstrumentType.GAUGE,
            3, 57, 15, 42, 42, null);
        ExecutionMetricSample mm = buildMetricMeasurement(snapshot);

        handler.processMetrics(null, List.of(mm));
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
            "response_time_ms", Map.of(), InstrumentType.HISTOGRAM,
            2, 300, 100, 200, 200, dist);
        ExecutionMetricSample mm = buildMetricMeasurement(snapshot);

        handler.processMetrics(null, List.of(mm));
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
            "cpu", Map.of("env", "qa", "region", "us-east"), InstrumentType.GAUGE,
            1, 80, 80, 80, 80, null);
        ExecutionMetricSample mm = buildMetricMeasurement(snapshot);

        handler.processMetrics(null, List.of(mm));
        handler.flush();

        List<Bucket> buckets = allBuckets();
        Assert.assertEquals(1, buckets.size());
        Assert.assertFalse(buckets.get(0).getAttributes().containsKey("region"));
        Assert.assertEquals("qa", buckets.get(0).getAttributes().get("env"));
    }

    // ── Artefact hash ────────────────────────────────────────────────────────

    /**
     * In exclude-mode, the production default, the artefact hash of the report node the sample was
     * produced by is ingested as a bucket dimension, for both metrics and measurements.
     */
    @Test
    public void artefactHashIsIngestedAsBucketAttribute() {
        TimeSeriesMetricSamplesHandler excludeModeHandler = newExcludeModeHandler();

        MetricSample snapshot = new MetricSample(0L,
            "requests", Map.of(), InstrumentType.COUNTER, 1, 1, 1, 1, 1, null);
        excludeModeHandler.processMetrics(null, List.of(buildMetricMeasurement(snapshot)));

        Measurement measurement = new Measurement();
        measurement.setExecId("exec-1");
        measurement.setName("myKeyword");
        measurement.setBegin(0L);
        measurement.setValue(1L);
        measurement.addCustomField(MetricsExecutionPlugin.ARTEFACT_HASH, "HASH-2");
        excludeModeHandler.processMeasurements(null, List.of(measurement));

        excludeModeHandler.flush();

        Set<Object> hashes = allBuckets().stream()
            .map(b -> b.getAttributes().get(MetricsExecutionPlugin.ARTEFACT_HASH))
            .collect(Collectors.toSet());
        Assert.assertEquals(Set.of("HASH-1", "HASH-2"), hashes);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TimeSeriesMetricSamplesHandler newExcludeModeHandler() {
        bucketsCollection = new InMemoryCollection<>();
        TimeSeriesCollection tsCollection = new TimeSeriesCollection(bucketsCollection, BUCKET_RESOLUTION);
        TimeSeries timeSeries = new TimeSeriesBuilder().registerCollection(tsCollection).build();
        return new TimeSeriesMetricSamplesHandler(timeSeries, Set.of(), Set.of("begin", "value"));
    }

    private ExecutionMetricSample buildMetricMeasurement(MetricSample snapshot) {
        return new ExecutionMetricSample(
            snapshot,
            "exec-1",        // execId
            "rn-1",          // rnId
            "HASH-1",        // artefactHash
            "plan-1",        // planId
            "MyPlan",        // plan name
            "canonical",     // canonicalPlanName
            "",              // taskId
            "",              // schedule
            "",              // execution description
            null,            // agentUrl
            null,            // origin
            null,            // additionalAttributes
            null             // metricType
        );
    }

    private List<Bucket> allBuckets() {
        return bucketsCollection.find(
            Filters.empty(), null, null, null, 0
        ).collect(Collectors.toList());
    }
}
