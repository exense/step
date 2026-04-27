package step.core.metrics;

import java.util.Map;
import java.util.TreeMap;

/**
 * Controller-level metric envelope: carries a {@link MetricSample} without any
 * execution context (no eId, planId, agentUrl, …).
 * <p>
 * Typical uses: grid token capacity/utilisation, system-level gauges.
 * <p>
 * {@link #getEffectiveLabels()} returns only the labels declared on the underlying
 * {@link MetricSample}, so handlers receive exactly the dimensions supplied by the
 * metric producer.
 */
public class ControllerMetricSample extends AbstractMetricSample {

    public ControllerMetricSample(MetricSample sample, String metricType) {
        super(sample, metricType);
    }

    @Override
    public Map<String, String> getEffectiveLabels() {
        return new TreeMap<>(sample.getLabels());
    }
}
