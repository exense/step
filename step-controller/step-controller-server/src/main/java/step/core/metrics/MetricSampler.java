package step.core.metrics;

import java.util.List;
import java.util.Objects;

public abstract class MetricSampler {
    public String name;
    public String description;

    public MetricSampler(String name, String description) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
    }

    /**
     * Return the latest samples collected by this sampler.
     *
     * @return typed controller-level metric snapshots, never {@code null}
     */
    public abstract List<ControllerMetricSample> collectMetricSamples();
}
