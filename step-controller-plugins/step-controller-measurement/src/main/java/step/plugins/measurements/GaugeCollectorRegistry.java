package step.plugins.measurements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

public class GaugeCollectorRegistry {

	private static final Logger logger = LoggerFactory.getLogger(GaugeCollectorRegistry.class);

	private static GaugeCollectorRegistry INSTANCE = new GaugeCollectorRegistry();

	public static GaugeCollectorRegistry getInstance() {
		return INSTANCE;
	}

	private final ScheduledExecutorService scheduler =
			Executors.newScheduledThreadPool(1);

	Map<String, GaugeCollector> collectors = new HashMap<>();
	List<MeasurementHandler> handlers = new ArrayList<MeasurementHandler>();

	public synchronized void registerCollector(String name, GaugeCollector collector){
		collectors.put(name, collector);
	}

	public synchronized void  registerHandler(MeasurementHandler handler){
		handlers.add(handler);
	}

	public GaugeCollector getGaugeCollector(String name) {
		return collectors.get(name);
	}

	public void start(int interval) {
		Runnable collect = () -> {
			try {
				collectors.forEach((c, t) -> {
					handlers.forEach(h -> h.processGauges(t.collectAsMeasurements()));
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
