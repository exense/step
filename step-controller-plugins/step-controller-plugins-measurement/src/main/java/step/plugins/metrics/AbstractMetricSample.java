package step.plugins.metrics;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import step.core.accessors.AbstractOrganizableObject;
import step.core.metrics.MetricSample;

import java.util.Map;

/**
 * Base class for all metric envelopes dispatched via
 * {@link MetricSamplesHandler#processMetrics} and {@link MetricSamplesHandler#processControllerMetrics}.
 * <p>
 * Extends {@link AbstractOrganizableObject} so that subclasses (in particular
 * {@link ExecutionMetricSample}) can be persisted by framework accessors that require an
 * {@code _id} field.
 * <p>
 * Two concrete subtypes exist:
 * <ul>
 *   <li>{@link ExecutionMetricSample} — carries full execution context (eId, planId, …)</li>
 *   <li>{@link ControllerMetricSample} — carries no execution context; used for
 *       controller-level metrics such as grid token usage.</li>
 * </ul>
 */
@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS)
@JsonSubTypes(value = {
    @JsonSubTypes.Type(ControllerMetricSample.class),
    @JsonSubTypes.Type(ExecutionMetricSample.class),
})
public abstract class AbstractMetricSample extends AbstractOrganizableObject {

    public static final String METRIC_TYPE = "metricType";

    public final MetricSample sample;

    /**
     * Optional time-series category override. When non-null, handlers that write a
     * {@code metricType} attribute use this value instead of
     * {@code sample.getType().toLowerCase()}.
     */
    public final String metricType;

    protected AbstractMetricSample(MetricSample sample, String metricType) {
        this.sample = sample;
        this.metricType = metricType;
    }

    /**
     * Returns the flat label map that handlers should use when writing this metric to
     * their respective storage.  The map includes both the developer-defined labels on
     * the underlying {@link MetricSample} and any context-specific labels added by the
     * concrete subclass (e.g. execution ID, agent URL).
     */
    public abstract Map<String, String> getEffectiveLabels();
}
