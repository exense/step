package step.plugins.metrics;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.metrics.MetricSample;
import step.core.metrics.InstrumentType;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Periodically re-emits the last known {@link ExecutionMetricSample} for each active
 * COUNTER and GAUGE metric to handlers that require a continuous stream of data points
 * (e.g. RAW storage, time-series) so that charts can plot values during intervals
 * where no new observation arrived from the agent.
 *
 * <p>A heartbeat sample is only emitted for a given metric when no real sample was
 * received during the last interval, avoiding duplicate writes when observations
 * are already frequent.
 *
 * <p>HISTOGRAM metrics are deliberately excluded: repeating a distribution snapshot
 * is semantically misleading because it implies new observations occurred.
 *
 * <p>Handlers opt-in by calling {@link #registerHandler(MetricSamplesHandler)}, typically
 * in their constructor. Prometheus is intentionally excluded because it retains values
 * in-memory between scrapes and would double-count counter increments if heartbeated.
 */
public class MetricHeartbeatRegistry {

    private static final Logger logger = LoggerFactory.getLogger(MetricHeartbeatRegistry.class);

    private static final MetricHeartbeatRegistry INSTANCE = new MetricHeartbeatRegistry();

    public static MetricHeartbeatRegistry getInstance() {
        return INSTANCE;
    }

    private final ConcurrentHashMap<String, LastSampleEntry> lastSamples = new ConcurrentHashMap<>();
    private final List<MetricSamplesHandler> handlers = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1, new BasicThreadFactory.Builder()
                    .namingPattern("metric-heartbeat-%d").build());
    volatile long intervalMs;

    private static final class LastSampleEntry {
        final ExecutionMetricSample sample;
        final long lastUpdatedMs;

        LastSampleEntry(ExecutionMetricSample sample, long lastUpdatedMs) {
            this.sample = sample;
            this.lastUpdatedMs = lastUpdatedMs;
        }
    }

    public void registerHandler(MetricSamplesHandler handler) {
        handlers.add(handler);
    }

    /**
     * Records the last known sample for the given metric. Called from
     * {@link SamplesExecutionPlugin#processMetrics} after each real dispatch.
     * HISTOGRAM samples are silently ignored.
     */
    public void update(ExecutionMetricSample executionMetricSample) {
        if (executionMetricSample.sample.getType() == InstrumentType.HISTOGRAM) {
            return;
        }
        String key = buildKey(executionMetricSample);
        lastSamples.put(key, new LastSampleEntry(executionMetricSample, System.currentTimeMillis()));
    }

    /**
     * Removes all tracked samples for the given execution.
     * Must be called when an execution ends to prevent stale heartbeats.
     */
    public void removeExecution(String execId) {
        String prefix = execId + "|";
        lastSamples.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /**
     * Starts the periodic heartbeat ticker.
     *
     * @param intervalMs tick interval in milliseconds; also used as the staleness
     *                   threshold — a heartbeat is only emitted if no real sample
     *                   arrived within this duration
     */
    public void start(long intervalMs) {
        this.intervalMs = intervalMs;
        scheduler.scheduleAtFixedRate(this::tick, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    void tick() {
        try {
            long now = System.currentTimeMillis();
            List<ExecutionMetricSample> heartbeats = lastSamples.values().stream()
                    .filter(entry -> now - entry.lastUpdatedMs >= intervalMs)
                    .map(entry -> buildHeartbeat(entry.sample, now))
                    .collect(Collectors.toList());
            if (!heartbeats.isEmpty()) {
                for (MetricSamplesHandler handler : handlers) {
                    try {
                        handler.processMetrics(heartbeats);
                    } catch (Exception e) {
                        logger.error("Error dispatching metric heartbeats to {}", handler.getClass().getSimpleName(), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in metric heartbeat tick", e);
        }
    }

    private static String buildKey(ExecutionMetricSample mm) {
        String execId = mm.eId != null ? mm.eId : "";
        String agentUrl = mm.agentUrl != null ? mm.agentUrl : "";
        String origin = mm.origin != null ? mm.origin : "";
        Map<String, String> labels = mm.sample.getLabels();
        String labelsKey = labels != null ? new TreeMap<>(labels).toString() : "{}";
        return execId + "|" + agentUrl + "|" + origin + "|" + mm.sample.getName() + "|" + labelsKey;
    }

    /**
     * Builds a synthetic heartbeat sample at {@code now} from the last known real sample.
     *
     * <ul>
     *   <li><b>COUNTER</b>: {@code count=0} (no new increments), {@code sum/min/max/last =
     *       runningTotal} — consistent with counter bucket semantics where LAST/MAX aggregation
     *       gives the current absolute total and RATE remains 0 for empty intervals.</li>
     *   <li><b>GAUGE</b>: {@code count=1, sum/min/max/last = lastObservedValue} — repeats
     *       the last known value as a single synthetic observation.</li>
     * </ul>
     */
    private static ExecutionMetricSample buildHeartbeat(ExecutionMetricSample original, long now) {
        MetricSample orig = original.sample;
        MetricSample heartbeatSample;
        if (orig.getType() == InstrumentType.COUNTER) {
            long runningTotal = orig.getSum();
            heartbeatSample = new MetricSample(now, orig.getName(), orig.getLabels(), InstrumentType.COUNTER,
                    0, runningTotal, runningTotal, runningTotal, runningTotal, null);
        } else {
            // GAUGE
            long last = orig.getLast();
            heartbeatSample = new MetricSample(now, orig.getName(), orig.getLabels(), InstrumentType.GAUGE,
                    1, last, last, last, last, null);
        }
        return new ExecutionMetricSample(heartbeatSample, original.eId, original.rnId, original.planId,
                original.plan, original.taskId, original.schedule, original.execution,
                original.agentUrl, original.origin, original.getAttributes(), original.metricType);
    }
}
