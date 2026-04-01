package step.plugins.measurements;

import io.prometheus.client.Counter;
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

    Map<String, Counter> counterCollectors = new HashMap<>();
    Map<String, Histogram> histogramCollectors = new HashMap<>();
    Map<String, Gauge> gaugeCollectors = new HashMap<>();

    public synchronized Counter getOrCreateCounter(String name, String help, String... labels) {
        return counterCollectors.computeIfAbsent(name, n ->
            Counter.build().name(n).help(help).labelNames(labels).register());
    }

    public synchronized Histogram getOrCreateHistogram(String name, String help, double[] buckets, String... labels) {
        return histogramCollectors.computeIfAbsent(name, n -> {
            Histogram.Builder builder = Histogram.build().name(n).help(help).labelNames(labels);
            if (buckets != null && buckets.length > 0) {
                builder.buckets(buckets);
            }
            return builder.register();
        });
    }

    public boolean containsHistogram(String name) {
        return histogramCollectors.containsKey(name);
    }

    public synchronized Gauge getOrCreateGauge(String name, String help, String... labels) {
        return gaugeCollectors.computeIfAbsent(name, n ->
            Gauge.build().name(n).help(help).labelNames(labels).register());
    }

}
