package step.plugins.measurements;

import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class PrometheusCollectorRegistry {

	private static final Logger logger = LoggerFactory.getLogger(PrometheusCollectorRegistry.class);

	private static PrometheusCollectorRegistry INSTANCE = new PrometheusCollectorRegistry();

	public static PrometheusCollectorRegistry getInstance() {
		return INSTANCE;
	}

	Map<String, Histogram> histoCollectors = new HashMap<>();
	Map<String, Gauge> gaugeCollectors = new HashMap<>();

	public synchronized Histogram getOrCreateHistogram(String name, String help, double[] buckets, String... labels) {
		Histogram histo;
		if (histoCollectors.containsKey(name)) {
			histo = histoCollectors.get(name);
		} else {
			Histogram.Builder builder = Histogram.build()
					.name(name).help(help)
					.labelNames(labels);
			if (buckets != null && buckets.length > 0) {
				builder.buckets(buckets);
			}
			histo = builder.register();
			histoCollectors.put(name, histo);
		}
		return histo;
	}

	public boolean containsHistogram(String name) {
		return histoCollectors.containsKey(name);
	}

	public synchronized Gauge getOrCreateGauge(String name, String help, String... labels) {
		Gauge gauge;
		if (gaugeCollectors.containsKey(name)) {
			gauge = gaugeCollectors.get(name);
		} else {
			Gauge.Builder builder = Gauge.build()
					.name(name).help(help)
					.labelNames(labels);
			gauge = builder.register();
			gaugeCollectors.put(name, gauge);
		}
		return gauge;
	}

}
