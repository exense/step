package step.plugins.measurements.raw;

import org.junit.Before;
import org.junit.Test;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.metrics.MetricSample;
import step.core.metrics.InstrumentType;
import step.plugins.measurements.ExecutionMetricSample;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class RawSamplesServicesTest {

    private static final String RN_ID = "rnId-1";
    private static final String EXEC_ID = "exec-1";
    private static final String PLAN_ID = "plan-1";

    private MetricSampleAccessor accessor;
    private RawSamplesServices service;

    @Before
    public void setUp() {
        accessor = new MetricSampleAccessor(new InMemoryCollection<>());
        service = new RawSamplesServices(accessor);
    }

    // -------------------------------------------------------------------------
    // getAggregatedMetricSamples — integration of stream + registry
    // -------------------------------------------------------------------------

    @Test
    public void testEmptyResult() {
        List<MetricSample> result = service.getAggregatedMetricSamples(RN_ID);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSingleSampleReturnedAsIs() {
        MetricSample sample = histogram("resp", Map.of(), 1000, 5, 25000, 3000, 7000, 6000);
        save(RN_ID, sample);

        List<MetricSample> result = service.getAggregatedMetricSamples(RN_ID);

        assertEquals(1, result.size());
        MetricSample agg = result.get(0);
        assertEquals("resp", agg.getName());
        assertEquals(5, agg.getCount());
        assertEquals(25000, agg.getSum());
        assertEquals(3000, agg.getMin());
        assertEquals(7000, agg.getMax());
    }

    @Test
    public void testMultipleSamplesOfSameMetricAggregated() {
        save(RN_ID, histogram("resp", Map.of(), 1000, 3, 15000, 3000, 6000, 5000));
        save(RN_ID, histogram("resp", Map.of(), 2000, 7, 42000, 2000, 9000, 8000));

        List<MetricSample> result = service.getAggregatedMetricSamples(RN_ID);

        assertEquals(1, result.size());
        MetricSample agg = result.get(0);
        assertEquals(10, agg.getCount());           // 3 + 7
        assertEquals(57000, agg.getSum());          // 15000 + 42000
        assertEquals(2000, agg.getMin());           // min(3000, 2000)
        assertEquals(9000, agg.getMax());           // max(6000, 9000)
        assertEquals(8000, agg.getLast());          // from more recent sample (t=2000)
        assertEquals(2000, agg.getSampleTime());    // max(1000, 2000)
    }

    @Test
    public void testDifferentMetricsReturnedAsSeparateEntries() {
        save(RN_ID, histogram("resp", Map.of(), 1000, 5, 25000, 3000, 7000, 6000));
        save(RN_ID, gauge("cpu", Map.of(), 2000, 2, 160, 70, 90, 85));

        List<MetricSample> result = service.getAggregatedMetricSamples(RN_ID);

        assertEquals(2, result.size());
    }

    @Test
    public void testSamplesForDifferentRnIdNotIncluded() {
        save(RN_ID, histogram("resp", Map.of(), 1000, 5, 25000, 3000, 7000, 6000));
        save("other-rn", histogram("resp", Map.of(), 2000, 3, 15000, 2000, 8000, 7000));

        List<MetricSample> result = service.getAggregatedMetricSamples(RN_ID);

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getCount()); // only the RN_ID sample
    }

    @Test
    public void testSameMetricNameDifferentLabelsAreDistinct() {
        save(RN_ID, histogram("resp", Map.of("env", "prod"), 1000, 5, 25000, 3000, 7000, 6000));
        save(RN_ID, histogram("resp", Map.of("env", "staging"), 2000, 3, 15000, 2000, 8000, 7000));

        List<MetricSample> result = service.getAggregatedMetricSamples(RN_ID);

        assertEquals(2, result.size());
    }

    // -------------------------------------------------------------------------
    // mergeSamples — COUNTER semantics
    // -------------------------------------------------------------------------

    @Test
    public void testMergeCounterAccumulatesDiffs() {
        MetricSample s1 = counter("events", Map.of(), 1000, 5, 100);
        MetricSample s2 = counter("events", Map.of(), 2000, 3, 103);

        MetricSample merged = RawSamplesServices.mergeSamples(s1, s2);

        assertEquals(InstrumentType.COUNTER, merged.getType());
        assertEquals(8, merged.getSum());       // 5 + 3
    }

    @Test
    public void testMergeCounterRunningTotalFromMoreRecentSample() {
        // s2 is more recent (higher sampleTime) — its sum (running total) wins
        MetricSample s1 = counter("events", Map.of(), 1000, 5, 100);
        MetricSample s2 = counter("events", Map.of(), 2000, 3, 103);

        MetricSample merged = RawSamplesServices.mergeSamples(s1, s2);

        assertEquals(8, merged.getSum());
        assertEquals(95, merged.getMin());
        assertEquals(103, merged.getMax());
        assertEquals(103, merged.getLast());
        assertNull(merged.getDistribution());
    }

    @Test
    public void testMergeCounterRunningTotalFromExistingWhenItIsMoreRecent() {
        // s1 is more recent — its sum wins even though it is "existing"
        MetricSample s1 = counter("events", Map.of(), 3000, 5, 200);
        MetricSample s2 = counter("events", Map.of(), 1000, 3, 100);

        MetricSample merged = RawSamplesServices.mergeSamples(s1, s2);

        assertEquals(8, merged.getSum());
        assertEquals(200, merged.getMax());  // s1 is more recent
        assertEquals(200, merged.getLast());  // s1 is more recent
    }

    // -------------------------------------------------------------------------
    // mergeSamples — GAUGE semantics
    // -------------------------------------------------------------------------

    @Test
    public void testMergeGaugeAccumulatesStats() {
        MetricSample s1 = gauge("cpu", Map.of(), 1000, 2, 140, 60, 80, 75);
        MetricSample s2 = gauge("cpu", Map.of(), 2000, 3, 270, 70, 100, 95);

        MetricSample merged = RawSamplesServices.mergeSamples(s1, s2);

        assertEquals(InstrumentType.GAUGE, merged.getType());
        assertEquals(5, merged.getCount());    // 2 + 3
        assertEquals(410, merged.getSum());    // 140 + 270
        assertEquals(60, merged.getMin());     // min(60, 70)
        assertEquals(100, merged.getMax());    // max(80, 100)
    }

    @Test
    public void testMergeGaugeLastFromMoreRecentSample() {
        MetricSample s1 = gauge("cpu", Map.of(), 1000, 2, 140, 60, 80, 75);
        MetricSample s2 = gauge("cpu", Map.of(), 2000, 3, 270, 70, 100, 95);

        MetricSample merged = RawSamplesServices.mergeSamples(s1, s2);

        assertEquals(95, merged.getLast());    // s2 is more recent
        assertEquals(2000, merged.getSampleTime());
    }

    // -------------------------------------------------------------------------
    // mergeSamples — HISTOGRAM semantics
    // -------------------------------------------------------------------------

    @Test
    public void testMergeHistogramAccumulatesStats() {
        MetricSample s1 = histogram("resp", Map.of(), 1000, 4, 20000, 3000, 6000, 5500);
        MetricSample s2 = histogram("resp", Map.of(), 2000, 6, 42000, 4000, 9000, 8000);

        MetricSample merged = RawSamplesServices.mergeSamples(s1, s2);

        assertEquals(InstrumentType.HISTOGRAM, merged.getType());
        assertEquals(10, merged.getCount());
        assertEquals(62000, merged.getSum());
        assertEquals(3000, merged.getMin());
        assertEquals(9000, merged.getMax());
        assertEquals(8000, merged.getLast());
    }

    // -------------------------------------------------------------------------
    // mergeDistributions
    // -------------------------------------------------------------------------

    @Test
    public void testMergeDistributionsBothNull() {
        assertNull(RawSamplesServices.mergeDistributions(null, null));
    }

    @Test
    public void testMergeDistributionsOneNull() {
        Map<Long, Long> dist = Map.of(100L, 3L, 200L, 5L);

        Map<Long, Long> resultA = RawSamplesServices.mergeDistributions(dist, null);
        assertEquals(dist, resultA);

        Map<Long, Long> resultB = RawSamplesServices.mergeDistributions(null, dist);
        assertEquals(dist, resultB);
    }

    @Test
    public void testMergeDistributionsBucketCountsSummed() {
        Map<Long, Long> a = new HashMap<>();
        a.put(100L, 3L);
        a.put(200L, 5L);

        Map<Long, Long> b = new HashMap<>();
        b.put(200L, 2L);
        b.put(300L, 4L);

        Map<Long, Long> merged = RawSamplesServices.mergeDistributions(a, b);

        assertEquals(3L, (long) merged.get(100L));   // only in a
        assertEquals(7L, (long) merged.get(200L));   // 5 + 2
        assertEquals(4L, (long) merged.get(300L));   // only in b
        assertEquals(3, merged.size());
    }

    @Test
    public void testMergeHistogramDistributionsMerged() {
        Map<Long, Long> dist1 = new HashMap<>();
        dist1.put(1000L, 2L);
        dist1.put(2000L, 1L);

        Map<Long, Long> dist2 = new HashMap<>();
        dist2.put(2000L, 3L);
        dist2.put(3000L, 4L);

        MetricSample s1 = histogramWithDist("resp", Map.of(), 1000, 3, 5000, 1000, 3000, 2500, dist1);
        MetricSample s2 = histogramWithDist("resp", Map.of(), 2000, 7, 17000, 1500, 4000, 3500, dist2);

        MetricSample merged = RawSamplesServices.mergeSamples(s1, s2);

        Map<Long, Long> dist = merged.getDistribution();
        assertNotNull(dist);
        assertEquals(2L, (long) dist.get(1000L));   // only in s1
        assertEquals(4L, (long) dist.get(2000L));   // 1 + 3
        assertEquals(4L, (long) dist.get(3000L));   // only in s2
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void save(String rnId, MetricSample sample) {
        ExecutionMetricSample sms = new ExecutionMetricSample(sample, EXEC_ID, rnId, PLAN_ID,
                "myPlan", "", "", "my test", null, null, null, null);
        accessor.save(Collections.singletonList(sms));
    }

    private static MetricSample counter(String name, Map<String, String> labels,
                                        long sampleTime, long increment, long runningTotal) {
        return new MetricSample(sampleTime, name, labels, InstrumentType.COUNTER,
                1, increment, runningTotal-increment, runningTotal, runningTotal, null);
    }

    private static MetricSample gauge(String name, Map<String, String> labels,
                                      long sampleTime, long count, long sum, long min, long max, long last) {
        return new MetricSample(sampleTime, name, labels, InstrumentType.GAUGE,
                count, sum, min, max, last, null);
    }

    private static MetricSample histogram(String name, Map<String, String> labels,
                                          long sampleTime, long count, long sum, long min, long max, long last) {
        return new MetricSample(sampleTime, name, labels, InstrumentType.HISTOGRAM,
                count, sum, min, max, last, null);
    }

    private static MetricSample histogramWithDist(String name, Map<String, String> labels,
                                                  long sampleTime, long count, long sum, long min, long max, long last,
                                                  Map<Long, Long> distribution) {
        return new MetricSample(sampleTime, name, labels, InstrumentType.HISTOGRAM,
                count, sum, min, max, last, distribution);
    }
}
