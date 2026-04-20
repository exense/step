package step.plugins.metrics;

import java.util.List;

public abstract class MetricSampler {
    public String name;
    public String description;

    public MetricSampler(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Return the latest samples collected by this sampler.
     * @return typed controller-level metric snapshots, never {@code null}
     */
    public abstract List<ControllerMetricSample> collectMetricSamples();
}
