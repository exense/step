package step.plugins.timeseries;

import org.junit.Assert;
import org.junit.Test;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ExecutionNotice;
import step.core.execution.notices.ExecutionNoticeManager;
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

import static step.plugins.timeseries.TimeSeriesMetricSamplesHandler.CARDINALITY_LABEL_COUNT_NOTICE_TYPE_ID;
import static step.plugins.timeseries.TimeSeriesMetricSamplesHandler.CARDINALITY_NOTICE_TYPE_ID;
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
        return newHandler(maxUniqueLabelValues, null);
    }

    private TimeSeriesMetricSamplesHandler newHandler(int maxUniqueLabelValues, ExecutionNoticeManager noticeManager) {
        bucketsCollection = new InMemoryCollection<>();
        TimeSeriesCollection tsCollection = new TimeSeriesCollection(bucketsCollection, BUCKET_RESOLUTION);
        TimeSeries timeSeries = new TimeSeriesBuilder().registerCollection(tsCollection).build();
        return new TimeSeriesMetricSamplesHandler(timeSeries, HANDLED_ATTRIBUTES, Set.of(), maxUniqueLabelValues, noticeManager);
    }

    /**
     * Builds a handler in exclude mode (empty allowlist) so that arbitrary custom label <em>names</em> flow
     * through to the safeguard. In allowlist (include) mode the set of label names is already bounded by the
     * allowlist, so the label-count safeguard only matters in exclude mode (the default production mode).
     */
    private TimeSeriesMetricSamplesHandler newLabelCountHandler(int maxLabelsPerMetric, ExecutionNoticeManager noticeManager) {
        bucketsCollection = new InMemoryCollection<>();
        TimeSeriesCollection tsCollection = new TimeSeriesCollection(bucketsCollection, BUCKET_RESOLUTION);
        TimeSeries timeSeries = new TimeSeriesBuilder().registerCollection(tsCollection).build();
        // Value axis disabled (-1) to isolate the label-count safeguard.
        return new TimeSeriesMetricSamplesHandler(timeSeries, Set.of(), Set.of(), -1, maxLabelsPerMetric, noticeManager);
    }

    /**
     * Captures the notices the handler would raise, without needing a live execution context: the
     * handler passes its (possibly null) context straight through to {@link #raiseNotice}.
     */
    private static class CapturingNoticeManager extends ExecutionNoticeManager {
        final List<ExecutionNotice> captured = new ArrayList<>();

        CapturingNoticeManager() {
            super(100);
        }

        @Override
        public void raiseNotice(ExecutionContext executionContext, ExecutionNotice notice) {
            captured.add(notice);
        }
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

    // ── Disabled safeguard (default 3-arg constructor / value < 0) ─────────────────────────────

    @Test
    public void quotaDisabledIngestsEverything() {
        TimeSeriesMetricSamplesHandler handler = newHandler(-1);
        handler.processMetrics(null, metricsWithDistinctLabel("user_id", 25, "m1", "e1"));
        handler.flush();

        Set<String> values = bucketAttributeValues("user_id");
        Assert.assertEquals(25, values.size());
        Assert.assertFalse(values.contains(QUOTA_EXCEEDED_VALUE));
    }

    // ── Strictest value quota (0): every custom value is masked, but the label key is kept ──────

    @Test
    public void valueQuotaZeroMasksEveryValue() {
        TimeSeriesMetricSamplesHandler handler = newHandler(0);
        handler.processMetrics(null, metricsWithDistinctLabel("user_id", 5, "m1", "e1"));
        handler.flush();

        Set<String> values = bucketAttributeValues("user_id");
        // The label dimension is retained but collapses to the single placeholder.
        Assert.assertEquals(Set.of(QUOTA_EXCEEDED_VALUE), values);
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

    // ── A single notice is raised, once per (metric, label), when the quota is exceeded ─────────

    @Test
    public void raisesNoticeOncePerLabelWhenQuotaExceeded() {
        CapturingNoticeManager noticeManager = new CapturingNoticeManager();
        TimeSeriesMetricSamplesHandler handler = newHandler(2, noticeManager);

        // 5 distinct values on (m1, user_id) with quota 2 → quota exceeded, but the notice must be raised once.
        handler.processMetrics(null, metricsWithDistinctLabel("user_id", 5, "m1", "e1"));
        handler.flush();

        Assert.assertEquals(1, noticeManager.captured.size());
        ExecutionNotice notice = noticeManager.captured.get(0);
        Assert.assertEquals(CARDINALITY_NOTICE_TYPE_ID, notice.typeId());
        Assert.assertEquals("user_id", notice.parameters().get("labelName"));
        Assert.assertEquals("m1", notice.parameters().get("metricName"));
        Assert.assertEquals("2", notice.parameters().get("quota"));
    }

    @Test
    public void noNoticeRaisedWhenWithinQuota() {
        CapturingNoticeManager noticeManager = new CapturingNoticeManager();
        TimeSeriesMetricSamplesHandler handler = newHandler(20, noticeManager);
        handler.processMetrics(null, metricsWithDistinctLabel("status", 15, "m1", "e1"));
        handler.flush();
        Assert.assertTrue(noticeManager.captured.isEmpty());
    }

    // ── Label-count safeguard: distinct label names per metric are bounded ──────────────────────

    @Test
    public void excessLabelNamesAreDropped() {
        TimeSeriesMetricSamplesHandler handler = newLabelCountHandler(2, null);
        // 4 distinct label names on m1 with a label-count quota of 2 → only the first 2 names survive.
        handler.processMetrics(null, List.of(
            metricWithLabel("m1", "e1", "lbl-0", "v"),
            metricWithLabel("m1", "e1", "lbl-1", "v"),
            metricWithLabel("m1", "e1", "lbl-2", "v"),
            metricWithLabel("m1", "e1", "lbl-3", "v")));
        handler.flush();

        Set<String> keys = allBucketAttributeKeys();
        Assert.assertTrue(keys.contains("lbl-0"));
        Assert.assertTrue(keys.contains("lbl-1"));
        Assert.assertFalse("over-quota label name must be dropped", keys.contains("lbl-2"));
        Assert.assertFalse("over-quota label name must be dropped", keys.contains("lbl-3"));
    }

    @Test
    public void admittedLabelNamesKeepPassingAcrossBatches() {
        TimeSeriesMetricSamplesHandler handler = newLabelCountHandler(1, null);
        handler.processMetrics(null, List.of(metricWithLabel("m1", "e1", "lbl-0", "v")));
        // Second batch: lbl-0 is already admitted and must still pass; lbl-1 is new and over quota → dropped.
        handler.processMetrics(null, List.of(
            metricWithLabel("m1", "e1", "lbl-0", "v"),
            metricWithLabel("m1", "e1", "lbl-1", "v")));
        handler.flush();

        Set<String> keys = allBucketAttributeKeys();
        Assert.assertTrue(keys.contains("lbl-0"));
        Assert.assertFalse(keys.contains("lbl-1"));
    }

    @Test
    public void labelCountQuotaIsIsolatedPerMetricName() {
        TimeSeriesMetricSamplesHandler handler = newLabelCountHandler(1, null);
        handler.processMetrics(null, List.of(
            metricWithLabel("m1", "e1", "a", "v"),
            metricWithLabel("m1", "e1", "b", "v"), // dropped on m1
            metricWithLabel("m2", "e1", "c", "v"))); // first on m2, kept
        handler.flush();

        Set<String> keys = allBucketAttributeKeys();
        Assert.assertTrue(keys.contains("a"));
        Assert.assertFalse(keys.contains("b"));
        Assert.assertTrue(keys.contains("c"));
    }

    @Test
    public void labelCountQuotaDisabledKeepsAllLabelNames() {
        TimeSeriesMetricSamplesHandler handler = newLabelCountHandler(-1, null);
        handler.processMetrics(null, List.of(
            metricWithLabel("m1", "e1", "lbl-0", "v"),
            metricWithLabel("m1", "e1", "lbl-1", "v"),
            metricWithLabel("m1", "e1", "lbl-2", "v")));
        handler.flush();

        Set<String> keys = allBucketAttributeKeys();
        Assert.assertTrue(keys.contains("lbl-0"));
        Assert.assertTrue(keys.contains("lbl-1"));
        Assert.assertTrue(keys.contains("lbl-2"));
    }

    // ── Strictest label-count quota (0): all custom labels are dropped ──────────────────────────

    @Test
    public void labelCountZeroDropsAllCustomLabels() {
        TimeSeriesMetricSamplesHandler handler = newLabelCountHandler(0, null);
        handler.processMetrics(null, List.of(
            metricWithLabel("m1", "e1", "lbl-0", "v"),
            metricWithLabel("m1", "e1", "lbl-1", "v")));
        handler.flush();

        Set<String> keys = allBucketAttributeKeys();
        Assert.assertFalse(keys.contains("lbl-0"));
        Assert.assertFalse(keys.contains("lbl-1"));
        // The metric itself is still ingested, just without its custom labels.
        Assert.assertTrue(keys.contains("name"));
    }

    @Test
    public void raisesLabelCountNoticeOncePerMetric() {
        CapturingNoticeManager noticeManager = new CapturingNoticeManager();
        TimeSeriesMetricSamplesHandler handler = newLabelCountHandler(1, noticeManager);
        handler.processMetrics(null, List.of(
            metricWithLabel("m1", "e1", "a", "v"),
            metricWithLabel("m1", "e1", "b", "v"), // over quota on m1
            metricWithLabel("m1", "e1", "c", "v"), // over quota on m1 again → no second notice
            metricWithLabel("m2", "e1", "d", "v"),
            metricWithLabel("m2", "e1", "e", "v"))); // over quota on m2
        handler.flush();

        List<ExecutionNotice> labelCountNotices = noticeManager.captured.stream()
            .filter(n -> CARDINALITY_LABEL_COUNT_NOTICE_TYPE_ID.equals(n.typeId()))
            .collect(Collectors.toList());
        Assert.assertEquals("one notice per offending metric", 2, labelCountNotices.size());
        Set<String> noticedMetrics = labelCountNotices.stream()
            .map(n -> n.parameters().get("metricName"))
            .collect(Collectors.toSet());
        Assert.assertEquals(Set.of("m1", "m2"), noticedMetrics);
        labelCountNotices.forEach(n -> Assert.assertEquals("1", n.parameters().get("quota")));
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

    private ExecutionMetricSample metricWithLabel(String metricName, String execId, String labelName, String labelValue) {
        MetricSample snapshot = new MetricSample(0L, metricName,
            Map.of(labelName, labelValue), InstrumentType.GAUGE, 1, 1, 1, 1, 1, null);
        return new ExecutionMetricSample(snapshot, execId, "rn", "plan-1", "MyPlan",
            "canonical", "", "", "", null, null, null, null);
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

    private Set<String> allBucketAttributeKeys() {
        return allBuckets().stream()
            .flatMap(b -> b.getAttributes().keySet().stream())
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
