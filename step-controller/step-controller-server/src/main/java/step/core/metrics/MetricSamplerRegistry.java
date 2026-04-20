package step.core.metrics;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MetricSamplerRegistry {

    private static final Logger logger = LoggerFactory.getLogger(MetricSamplerRegistry.class);

    private static final MetricSamplerRegistry INSTANCE = new MetricSamplerRegistry();

    public static MetricSamplerRegistry getInstance() {
        return INSTANCE;
    }

    private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(1, BasicThreadFactory.builder().namingPattern("metric-sampler-%d").build());

    Map<String, MetricSampler> samplers = new ConcurrentHashMap<>();
    List<MetricSamplesHandler> handlers = new CopyOnWriteArrayList<>();

    public void registerSampler(String name, MetricSampler sampler) {
        samplers.put(name, sampler);
    }

    public void registerHandler(MetricSamplesHandler handler) {
        handlers.add(handler);
    }

    public void start(int interval) {
        Runnable collect = () -> {
            try {
                samplers.forEach((c, t) -> {
                    try {
                        List<ControllerMetricSample> controllerMetrics = t.collectMetricSamples();
                            if (!controllerMetrics.isEmpty()) {
                                handlers.forEach(h -> {
                                    try {
                                        h.processControllerMetrics(controllerMetrics);
                                    } catch (Exception e) {
                                        logger.error("Exception occurred while processing metric sampling.", e);
                                    }
                                });
                            }
                    } catch (Exception e) {
                        logger.error("Exception occurred while collecting metric samples.", e);
                    }
                });
            } catch (Exception e) {
                logger.error("Exception occurred during metric sampling.", e);
            }
        };
        scheduler.scheduleAtFixedRate(collect, 15, interval, SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }
}
