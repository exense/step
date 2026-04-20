package step.plugins.metrics;

import step.core.timeseries.metric.MetricType;
import step.core.timeseries.metric.MetricTypeAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetricTypeRegistry {

    private final Map<String, MetricType> metrics = new ConcurrentHashMap<>();
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
        return new ArrayList<>(metrics.values());
    }

    public MetricType getMetricType(String name) {
        return metrics.get(name);
    }
}
