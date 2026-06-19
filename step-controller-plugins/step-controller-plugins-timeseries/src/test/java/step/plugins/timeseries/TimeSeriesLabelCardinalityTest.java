package step.plugins.timeseries;

import org.junit.Assert;
import org.junit.Test;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.metrics.ExecutionMetricSample;
import step.core.metrics.InstrumentType;
import step.core.metrics.Measurement;
import step.core.metrics.MetricSample;
import step.core.timeseries.TimeSeries;
import step.core.timeseries.TimeSeriesBuilder;
import step.core.timeseries.TimeSeriesCollection;
import step.core.timeseries.bucket.Bucket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static step.plugins.timeseries.TimeSeriesMetricSamplesHandler.QUOTA_EXCEEDED_VALUE;

/**
 * Verifies the per-execution label cardinality safeguard implemented by
 * {@link TimeSeriesMetricSamplesHandler}.
 */
public class TimeSeriesLabelCardinalityTest {

    private static final int BUCKET_RESOLUTION = 1000;
    // Exclude-mode disabled; only these attributes are kept as bucket dimensions.
    private static final Set<String> HANDLED_ATTRIBUTES = Set.of("eId", "name", "user_id", "status");

    private InMemoryCollection<Bucket> bucketsCollection;

    private TimeSeriesMetricSamplesHandler newHandler(int maxUniqueLabelValues) {
        bucketsCollection = new InMemoryCollection<>();
        TimeSeriesCollection tsCollection = new TimeSeriesCollection(bucketsCollection, BUCKET_RESOLUTION);
        TimeSeries timeSeries = new TimeSeriesBuilder().registerCollection(tsCollection).build();
        return new TimeSeriesMetricSamplesHandler(timeSeries, HANDLED_ATTRIBUTES, Set.of(), maxUniqueLabelValues);
    }

    // ── Normal behavior: all unique values below the quota are ingested unchanged ───────────────

    @Test
    public void uniqueValuesBelowQuotaAreAllIngested() {
        TimeSeriesMetricSamplesHandler handler = newHandler(20);
        handler.processMetrics(null, metricsWithDistinctLabel("status", 15, "m1", "e1"));
        handler.flush();

        Set<String> statusValues = bucketAttributeValues("status");
        Assert.assertEquals(15, statusValues.size());
        Assert.assertFalse(statusValues.contains(QUOTA_EXCEEDED_VALUE));
    }

    // ── Quota exceeded: values beyond the quota are masked ─────────────────────────────────────

    @Test
    public void uniqueValuesBeyondQuotaAreMasked() {
        TimeSeriesMetricSamplesHandler handler = newHandler(20);
        handler.processMetrics(null, metricsWithDistinctLabel("user_id", 25, "m1", "e1"));
        handler.flush();

        Set<String> values = bucketAttributeValues("user_id");
        // 20 originals kept + 1 collapsed masked value
        Assert.assertEquals(21, values.size());
        Assert.assertTrue(values.contains(QUOTA_EXCEEDED_VALUE));
        Assert.assertTrue(values.contains("user_id-0"));
        Assert.assertTrue(values.contains("user_id-19"));
        // The 21st..25th unique originals must have been dropped
        Assert.assertFalse(values.contains("user_id-20"));
        Assert.assertFalse(values.contains("user_id-24"));
    }

    // ── Scope isolation: the quota is tracked per metric name ──────────────────────────────────

    @Test
    public void quotaIsIsolatedPerMetricName() {
        TimeSeriesMetricSamplesHandler handler = newHandler(2);
        List<ExecutionMetricSample> all = new ArrayList<>();
        all.addAll(metricsWithDistinctLabel("user_id", 3, "m1", "e1")); // m1 exceeds (3 > 2)
        all.addAll(metricsWithDistinctLabel("user_id", 2, "m2", "e1")); // m2 within quota
        handler.processMetrics(null, all);
        handler.flush();

        Set<String> m1Values = bucketAttributeValuesForMetric("user_id", "m1");
        Set<String> m2Values = bucketAttributeValuesForMetric("user_id", "m2");
        Assert.assertTrue("m1 should have been masked", m1Values.contains(QUOTA_EXCEEDED_VALUE));
        Assert.assertFalse("m2 must be untouched", m2Values.contains(QUOTA_EXCEEDED_VALUE));
        Assert.assertEquals(Set.of("user_id-0", "user_id-1"), m2Values);
    }

    // ── Disabled safeguard (default 3-arg constructor / value <= 0) ────────────────────────────

    @Test
    public void quotaDisabledIngestsEverything() {
        TimeSeriesMetricSamplesHandler handler = newHandler(0);
        handler.processMetrics(null, metricsWithDistinctLabel("user_id", 25, "m1", "e1"));
        handler.flush();

        Set<String> values = bucketAttributeValues("user_id");
        Assert.assertEquals(25, values.size());
        Assert.assertFalse(values.contains(QUOTA_EXCEEDED_VALUE));
    }

    // ── Measurement path is guarded as well ────────────────────────────────────────────────────

    @Test
    public void measurementCustomLabelsAreGuarded() {
        TimeSeriesMetricSamplesHandler handler = newHandler(2);
        List<Measurement> measurements = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            measurements.add(measurementWithLabel("m1", "e1", "user_id", "user_id-" + i));
        }
        handler.processMeasurements(null, measurements);
        handler.flush();

        Set<String> values = bucketAttributeValues("user_id");
        Assert.assertEquals(3, values.size()); // 2 originals + masked
        Assert.assertTrue(values.contains(QUOTA_EXCEEDED_VALUE));
        Assert.assertTrue(values.contains("user_id-0"));
        Assert.assertTrue(values.contains("user_id-1"));
        Assert.assertFalse(values.contains("user_id-2"));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────────────────────

    private List<ExecutionMetricSample> metricsWithDistinctLabel(String labelName, int count, String metricName, String execId) {
        List<ExecutionMetricSample> samples = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MetricSample snapshot = new MetricSample(0L, metricName,
                Map.of(labelName, labelName + "-" + i), InstrumentType.GAUGE, 1, 1, 1, 1, 1, null);
            samples.add(new ExecutionMetricSample(snapshot, execId, "rn-" + i, "plan-1", "MyPlan",
                "canonical", "", "", "", null, null, null, null));
        }
        return samples;
    }

    private Measurement measurementWithLabel(String metricName, String execId, String labelName, String labelValue) {
        Measurement m = new Measurement();
        m.setExecId(execId);
        m.setName(metricName);
        m.setBegin(0L);
        m.setValue(1L);
        m.addCustomField(labelName, labelValue);
        m.setCustomMetricLabelKeys(Set.of(labelName));
        return m;
    }

    private List<Bucket> allBuckets() {
        return bucketsCollection.find(Filters.empty(), null, null, null, 0).collect(Collectors.toList());
    }

    private Set<String> bucketAttributeValues(String attribute) {
        return allBuckets().stream()
            .map(b -> (String) b.getAttributes().get(attribute))
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private Set<String> bucketAttributeValuesForMetric(String attribute, String metricName) {
        return allBuckets().stream()
            .filter(b -> metricName.equals(b.getAttributes().get("name")))
            .map(b -> (String) b.getAttributes().get(attribute))
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
    }
}
