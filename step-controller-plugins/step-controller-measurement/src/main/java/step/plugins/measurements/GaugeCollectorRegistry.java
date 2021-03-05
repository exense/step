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
	List<AbstractMeasurementPlugin> handlers = new ArrayList<>();

	public synchronized void registerCollector(String name, GaugeCollector collector){
		collectors.put(name, collector);
	}

	 public synchronized void  registerHandler(AbstractMeasurementPlugin handler){
		handlers.add(handler);
	}

	public void start() {
		Runnable collect = () -> {
			try {
				collectors.forEach((c, t) -> {
					//List<String> labelNames = t.getLabels(); lable names not required here, label values in GaugeMetric
					List<GaugeCollector.GaugeMetric> gaugeMetrics = t.collect();
					handlers.forEach(h -> h.processGauges(t, gaugeMetrics));
				});
			} catch (Exception e) {
				logger.error("Exception occurred while processing gauge metrics", e);
			}
		};
		ScheduledFuture<?> collectHandle =
				scheduler.scheduleAtFixedRate(collect, 15, 15, SECONDS);
	}
}
