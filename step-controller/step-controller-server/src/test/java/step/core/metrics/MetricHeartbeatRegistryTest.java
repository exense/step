package step.core.metrics;

import org.junit.Before;
import org.junit.Test;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.metrics.MetricSample;
import step.core.metrics.InstrumentType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link step.core.metrics.MetricHeartbeatRegistry}.
 *
 * <p>Each test creates its own registry instance to avoid singleton state leakage.
 * The package-private {@code tick()} method and {@code intervalMs} field are
 * accessed directly to control timing without real sleep.
 *
 * <ul>
 *   <li>{@code intervalMs = 0} → every stored entry is immediately stale</li>
 *   <li>{@code intervalMs = Long.MAX_VALUE / 2} → every stored entry is fresh</li>
 * </ul>
 */
public class MetricHeartbeatRegistryTest {

    private step.core.metrics.MetricHeartbeatRegistry registry;
    private List<step.core.metrics.ExecutionMetricSample> captured;

    @Before
    public void setUp() {
        registry = new step.core.metrics.MetricHeartbeatRegistry();
        captured = new ArrayList<>();
        registry.registerHandler(capturingHandler(captured));
    }

    // ── HISTOGRAM filtering ───────────────────────────────────────────────────

    @Test
    public void histogramUpdateIsIgnored() {
        registry.update(stepSample("exec-1", histogram("resp", 5, 25000)));
        registry.intervalMs = 0;
        registry.tick();
        assertTrue("HISTOGRAM should not produce a heartbeat", captured.isEmpty());
    }

    // ── COUNTER heartbeat semantics ───────────────────────────────────────────

    @Test
    public void counterHeartbeatHasZeroCountAndRunningTotal() {
        registry.update(stepSample("exec-1", counter("requests", 7, 100)));
        registry.intervalMs = 0;
        registry.tick();

        assertEquals(1, captured.size());
        MetricSample hb = captured.get(0).sample;
        assertEquals(InstrumentType.COUNTER, hb.getType());
        assertEquals(0,   hb.getCount()); // no new increments in a heartbeat interval
        assertEquals(100, hb.getSum());   // running total preserved
        assertEquals(100, hb.getMin());
        assertEquals(100, hb.getMax());
        assertEquals(100, hb.getLast());
    }

    // ── GAUGE heartbeat semantics ─────────────────────────────────────────────

    @Test
    public void gaugeHeartbeatHasCountOneAndLastValue() {
        registry.update(stepSample("exec-1", gauge("cpu", 3, 300, 80, 120, 110)));
        registry.intervalMs = 0;
        registry.tick();

        assertEquals(1, captured.size());
        MetricSample hb = captured.get(0).sample;
        assertEquals(InstrumentType.GAUGE, hb.getType());
        assertEquals(1,   hb.getCount()); // synthetic single observation
        assertEquals(110, hb.getSum());   // last known value
        assertEquals(110, hb.getMin());
        assertEquals(110, hb.getMax());
        assertEquals(110, hb.getLast());
    }

    // ── Staleness gate ────────────────────────────────────────────────────────

    @Test
    public void noHeartbeatWhenSampleUpdatedWithinInterval() {
        registry.update(stepSample("exec-1", gauge("mem", 1, 50, 50, 50, 50)));
        registry.intervalMs = Long.MAX_VALUE / 2; // entry will always be fresh
        registry.tick();
        assertTrue("No heartbeat expected for a fresh sample", captured.isEmpty());
    }

    @Test
    public void heartbeatEmittedWhenIntervalElapsed() {
        registry.update(stepSample("exec-1", gauge("mem", 1, 50, 50, 50, 50)));
        registry.intervalMs = 0; // entry immediately stale
        registry.tick();
        assertEquals(1, captured.size());
    }

    @Test
    public void freshUpdateSuppressesHeartbeatOnNextTick() {
        // First tick: entry stale → heartbeat
        registry.update(stepSample("exec-1", gauge("mem", 1, 50, 50, 50, 50)));
        registry.intervalMs = 0;
        registry.tick();
        assertEquals(1, captured.size());

        // Refresh the entry and use a huge interval → no longer stale
        registry.update(stepSample("exec-1", gauge("mem", 1, 60, 60, 60, 60)));
        registry.intervalMs = Long.MAX_VALUE / 2;
        captured.clear();
        registry.tick();
        assertTrue("No heartbeat expected after fresh update", captured.isEmpty());
    }

    // ── Heartbeat content ─────────────────────────────────────────────────────

    @Test
    public void heartbeatPreservesExecutionMetadata() {
        step.core.metrics.ExecutionMetricSample original = new step.core.metrics.ExecutionMetricSample(
                gauge("cpu", 1, 70, 70, 70, 70),
                "exec-42", "rn-99", "plan-7",
                "MyPlan", "task-1", "sched-1", "exec desc",
                "http://agent", "MyKeyword", null, null);
        registry.update(original);
        registry.intervalMs = 0;
        registry.tick();

        assertEquals(1, captured.size());
        step.core.metrics.ExecutionMetricSample hb = captured.get(0);
        assertEquals("exec-42", hb.eId);
        assertEquals("rn-99",   hb.rnId);
        assertEquals("plan-7",  hb.planId);
        assertEquals("MyPlan",  hb.plan);
        assertEquals("task-1",  hb.taskId);
    }

    @Test
    public void heartbeatTimestampIsUpdatedToNow() {
        long before = System.currentTimeMillis();
        registry.update(stepSample("exec-1", gauge("cpu", 1, 70, 70, 70, 70)));
        registry.intervalMs = 0;
        registry.tick();
        long after = System.currentTimeMillis();

        assertFalse(captured.isEmpty());
        long hbTime = captured.get(0).sample.getSampleTime();
        assertTrue("Heartbeat sampleTime should be >= start of tick", hbTime >= before);
        assertTrue("Heartbeat sampleTime should be <= end of tick",   hbTime <= after);
    }

    // ── removeExecution ───────────────────────────────────────────────────────

    @Test
    public void removeExecutionClearsOnlyMatchingExecution() {
        registry.update(stepSample("exec-1", counter("req", 3, 30)));
        registry.update(stepSample("exec-2", counter("req", 5, 50)));

        registry.removeExecution("exec-1");

        registry.intervalMs = 0;
        registry.tick();

        assertEquals("Only exec-2 entry should remain", 1, captured.size());
        assertEquals("exec-2", captured.get(0).eId);
    }

    @Test
    public void removeExecutionWithNoEntriesIsNoop() {
        registry.removeExecution("nonexistent-exec"); // must not throw
        registry.intervalMs = 0;
        registry.tick();
        assertTrue(captured.isEmpty());
    }

    // ── Multiple handlers ─────────────────────────────────────────────────────

    @Test
    public void allRegisteredHandlersReceiveHeartbeat() {
        List<step.core.metrics.ExecutionMetricSample> captured2 = new ArrayList<>();
        registry.registerHandler(capturingHandler(captured2));

        registry.update(stepSample("exec-1", gauge("cpu", 1, 80, 80, 80, 80)));
        registry.intervalMs = 0;
        registry.tick();

        assertEquals("First handler should receive the heartbeat",  1, captured.size());
        assertEquals("Second handler should receive the heartbeat", 1, captured2.size());
    }

    @Test
    public void noDispatchWhenNoStaleEntries() {
        // Nothing stored → tick must not dispatch anything
        registry.intervalMs = 0;
        registry.tick();
        assertTrue(captured.isEmpty());
    }

    // ── Multiple metrics / executions ─────────────────────────────────────────

    @Test
    public void multipleMetricsAllHeartbeated() {
        registry.update(stepSample("exec-1", counter("req", 5, 100)));
        registry.update(stepSample("exec-1", gauge("cpu", 2, 160, 70, 90, 85)));
        registry.intervalMs = 0;
        registry.tick();

        assertEquals(2, captured.size());
    }

    @Test
    public void differentLabelsSameNameAreDistinctEntries() {
        MetricSample g1 = new MetricSample(System.currentTimeMillis(), "cpu",
                Map.of("env", "prod"), InstrumentType.GAUGE, 1, 80, 80, 80, 80, null);
        MetricSample g2 = new MetricSample(System.currentTimeMillis(), "cpu",
                Map.of("env", "staging"), InstrumentType.GAUGE, 1, 40, 40, 40, 40, null);

        registry.update(stepSample("exec-1", g1));
        registry.update(stepSample("exec-1", g2));
        registry.intervalMs = 0;
        registry.tick();

        assertEquals("Each distinct label set should produce its own heartbeat", 2, captured.size());
    }

    @Test
    public void differentAgentUrlsAreDistinctEntries() {
        MetricSample sample = gauge("cpu", 1, 80, 80, 80, 80);

        registry.update(stepSampleFull("exec-1", sample, "http://agent-1", "MyKeyword"));
        registry.update(stepSampleFull("exec-1", sample, "http://agent-2", "MyKeyword"));
        registry.intervalMs = 0;
        registry.tick();

        assertEquals("Same metric on different agents should produce separate heartbeats", 2, captured.size());
    }

    @Test
    public void differentOriginsAreDistinctEntries() {
        MetricSample sample = gauge("cpu", 1, 80, 80, 80, 80);

        registry.update(stepSampleFull("exec-1", sample, "http://agent-1", "KeywordA"));
        registry.update(stepSampleFull("exec-1", sample, "http://agent-1", "KeywordB"));
        registry.intervalMs = 0;
        registry.tick();

        assertEquals("Same metric from different keywords should produce separate heartbeats", 2, captured.size());
    }

    @Test
    public void sameAgentAndOriginSameMetricMergesEntry() {
        MetricSample first  = gauge("cpu", 1, 80, 80, 80, 80);
        MetricSample second = gauge("cpu", 1, 90, 90, 90, 90);

        registry.update(stepSampleFull("exec-1", first,  "http://agent-1", "KeywordA"));
        registry.update(stepSampleFull("exec-1", second, "http://agent-1", "KeywordA"));
        registry.intervalMs = 0;
        registry.tick();

        // Second update overwrites first → only one heartbeat with last value
        assertEquals("Repeated updates to same key should produce a single heartbeat", 1, captured.size());
        assertEquals(90, captured.get(0).sample.getLast());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private step.core.metrics.ExecutionMetricSample stepSample(String execId, MetricSample sample) {
        return new step.core.metrics.ExecutionMetricSample(sample, execId, "rn-1", "plan-1",
                "MyPlan", "", "", "my execution", null, null, null, null);
    }

    private step.core.metrics.ExecutionMetricSample stepSampleFull(String execId, MetricSample sample,
                                                                   String agentUrl, String origin) {
        return new step.core.metrics.ExecutionMetricSample(sample, execId, "rn-1", "plan-1",
                "MyPlan", "", "", "my execution", agentUrl, origin, null, null);
    }

    private static MetricSample counter(String name, long count, long runningTotal) {
        return new MetricSample(System.currentTimeMillis(), name, Map.of(),
                InstrumentType.COUNTER, count, runningTotal, runningTotal, runningTotal, runningTotal, null);
    }

    private static MetricSample gauge(String name, long count, long sum, long min, long max, long last) {
        return new MetricSample(System.currentTimeMillis(), name, Map.of(),
                InstrumentType.GAUGE, count, sum, min, max, last, null);
    }

    private static MetricSample histogram(String name, long count, long sum) {
        return new MetricSample(System.currentTimeMillis(), name, Map.of(),
                InstrumentType.HISTOGRAM, count, sum, 0, sum, sum, null);
    }

    private static step.core.metrics.MetricSamplesHandler capturingHandler(List<step.core.metrics.ExecutionMetricSample> sink) {
        return new step.core.metrics.MetricSamplesHandler() {
            @Override
            public void initializeExecutionContext(ExecutionEngineContext ctx, ExecutionContext execCtx) {}
            @Override
            public void processMeasurements(List<step.core.metrics.Measurement> measurements) {}
            @Override
            public void processMetrics(List<step.core.metrics.ExecutionMetricSample> metrics) { sink.addAll(metrics); }
            @Override
            public void afterExecutionEnd(ExecutionContext context) {}
        };
    }
}
