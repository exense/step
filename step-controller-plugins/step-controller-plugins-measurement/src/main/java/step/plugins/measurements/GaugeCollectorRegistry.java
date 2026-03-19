package step.plugins.measurements;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Singleton registry that periodically samples all registered {@link GaugeCollector}s and
 * forwards the resulting {@link Measurement}s to all registered {@link MeasurementHandler}s.
 *
 * <h3>Purpose</h3>
 * <p>Gauge metrics (e.g. active thread-group counts, agent token usage) are maintained as
 * Prometheus client {@link io.prometheus.client.Gauge} objects whose values change at
 * arbitrary points in time. Without periodic sampling, a charting consumer that zooms into
 * a quiet time-window would see no data points even though the gauge had a non-zero value
 * throughout. By publishing the current gauge readings as {@link Measurement}s on a fixed
 * interval, downstream handlers (time-series, raw storage, Prometheus export, …) always
 * have up-to-date snapshots available.
 *
 * <h3>Usage</h3>
 * <ol>
 *   <li>Implement {@link GaugeCollector} and register it via
 *       {@link #registerCollector(String, GaugeCollector)}.</li>
 *   <li>Register one or more {@link MeasurementHandler}s via
 *       {@link #registerHandler(MeasurementHandler)}.</li>
 *   <li>Call {@link #start(int)} once to begin periodic sampling. The first sample is
 *       taken 15 seconds after start; subsequent samples follow the configured interval.</li>
 *   <li>Call {@link #stop()} on shutdown to release the background thread.</li>
 * </ol>
 */
public class GaugeCollectorRegistry {

    private static final Logger logger = LoggerFactory.getLogger(GaugeCollectorRegistry.class);

    private static GaugeCollectorRegistry INSTANCE = new GaugeCollectorRegistry();

    public static GaugeCollectorRegistry getInstance() {
        return INSTANCE;
    }

    private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(1, new BasicThreadFactory.Builder().namingPattern("gauge-collector-%d").build());

    Map<String, GaugeCollector> collectors = new ConcurrentHashMap<>();
    List<MeasurementHandler> handlers = new CopyOnWriteArrayList<>();

    /**
     * Registers a {@link GaugeCollector} under the given name.
     * Replaces any previously registered collector with the same name.
     */
    public void registerCollector(String name, GaugeCollector collector) {
        collectors.put(name, collector);
    }

    /**
     * Registers a {@link MeasurementHandler} that will receive the {@link Measurement}s
     * produced at each sampling tick.
     */
    public void registerHandler(MeasurementHandler handler) {
        handlers.add(handler);
    }

    /**
     * Returns the {@link GaugeCollector} registered under the given name, or {@code null}
     * if none has been registered.
     */
    public GaugeCollector getGaugeCollector(String name) {
        return collectors.get(name);
    }

    /**
     * Starts the periodic sampling loop.
     * <p>The first sample is taken 15 seconds after this call; subsequent samples are taken
     * every {@code interval} seconds. Each tick collects the current readings from all
     * registered {@link GaugeCollector}s and dispatches the resulting {@link Measurement}s
     * to all registered {@link MeasurementHandler}s.
     *
     * @param interval sampling interval in seconds
     */
    public void start(int interval) {
        Runnable collect = () -> {
            try {
                collectors.forEach((c, t) -> {
                    List<Measurement> measurements = t.collectAsMeasurements();
                    handlers.forEach(h -> h.processInternalGauges(measurements));
                });
            } catch (Exception e) {
                logger.error("Exception occurred while processing gauge metrics", e);
            }
        };
        ScheduledFuture<?> collectHandle =
            scheduler.scheduleAtFixedRate(collect, 15, interval, SECONDS);
    }

    /** Shuts down the sampling scheduler. Should be called on application shutdown. */
    public void stop() {
        scheduler.shutdown();
    }
}
