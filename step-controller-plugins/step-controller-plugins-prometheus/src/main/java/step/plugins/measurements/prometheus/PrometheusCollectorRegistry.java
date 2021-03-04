package step.plugins.measurements.prometheus;

import io.prometheus.client.Histogram;
import io.prometheus.client.SimpleCollector;
import step.common.managedoperations.OperationManager;

import java.util.HashMap;
import java.util.Map;

public class PrometheusCollectorRegistry {

	private static PrometheusCollectorRegistry INSTANCE = new PrometheusCollectorRegistry();

	public static PrometheusCollectorRegistry getInstance() {
		return INSTANCE;
	}

	Map<String, SimpleCollector> collectors = new HashMap<>();

	public synchronized Histogram getOrCreateHistogram(String name, String help, double[] buckets, String... labels) {
		Histogram histo;
		if (collectors.containsKey(name)) {
			histo = (Histogram) collectors.get(name);
		} else {
			Histogram.Builder builder = Histogram.build()
					.name("step_node_duration_seconds").help("step node duration in seconds.")
					.labelNames("eId", "name", "type", "status", "planId", "taskId");
			if (buckets != null && buckets.length > 0) {
				builder.buckets(buckets);
			}
			histo = builder.register();
			collectors.put(name, histo);
		}
		return histo;
	}
}
