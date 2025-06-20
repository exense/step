package step.plugins.measurements;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;

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

	public void registerCollector(String name, GaugeCollector collector){
		collectors.put(name, collector);
	}

	public void  registerHandler(MeasurementHandler handler){
		handlers.add(handler);
	}

	public GaugeCollector getGaugeCollector(String name) {
		return collectors.get(name);
	}

	public void start(int interval) {
		Runnable collect = () -> {
			try {
				collectors.forEach((c, t) -> {
					List<Measurement> measurements = t.collectAsMeasurements();
					handlers.forEach(h -> h.processGauges(measurements));
				});
			} catch (Exception e) {
				logger.error("Exception occurred while processing gauge metrics", e);
			}
		};
		ScheduledFuture<?> collectHandle =
				scheduler.scheduleAtFixedRate(collect, 15, interval, SECONDS);
	}

	public void stop() {
		scheduler.shutdown();
	}
}
