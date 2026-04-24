package step.core.metrics;

import step.core.timeseries.metric.MetricType;
import step.core.timeseries.metric.MetricTypeAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MetricTypeRegistry {

    //Preserve insertion order when returning the list of the metrics to clients
    private final Map<String, MetricType> metrics = Collections.synchronizedMap(new LinkedHashMap<>());
    private final MetricTypeAccessor metricTypeAccessor;

    public MetricTypeRegistry(MetricTypeAccessor metricTypeAccessor) {
        this.metricTypeAccessor = metricTypeAccessor;
    }

    public void registerMetricType(MetricType metricType) {
        metrics.put(metricType.getName(), metricType);
        MetricType existingMetric = metricTypeAccessor.findByCriteria(Map.of("name", metricType.getName()));
        if (existingMetric != null) {
            metricType.setId(existingMetric.getId()); // update the metric
        }
        metricTypeAccessor.save(metricType);
    }

    public List<MetricType> getMetrics() {
        // The explicit synchronized (metrics) block in getMetrics() is only needed because iterating over metrics.values() — which new ArrayList<>(...) does internally — is a multi-step operation that the wrapper doesn't protect atomically. From the Javadoc:
        // It is imperative that the user manually synchronize on the returned map when traversing any of its collection views via Iterator, Spliterator or Stream.
        synchronized (metrics) {
            return new ArrayList<>(metrics.values());
        }
    }

    public MetricType getMetricType(String name) {
        return metrics.get(name);
    }
}
