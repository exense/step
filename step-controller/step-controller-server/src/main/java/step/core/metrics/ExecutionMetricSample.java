package step.core.metrics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.TreeMap;

/**
 * Controller-side representation of a {@link MetricSample}, enriched with
 * execution context (execution ID, plan, agent URL, schedule, etc.) in the same way
 * as {@link Measurement} is enriched for legacy {@code Measure} objects.
 * <p>
 * Unlike {@link Measurement}, this is a typed flat POJO rather than a {@link java.util.HashMap}
 * subclass. Handlers receive lists of these via {@link MetricSamplesHandler#processMetrics}.
 */
public class ExecutionMetricSample extends StepMetricSample {

    public final String eId;
    public final String rnId;
    public final String planId;
    public final String plan;
    public final String canonicalPlanName;
    public final String taskId;
    public final String schedule;
    public final String execution;
    public final String agentUrl;
    public final String origin;

    @JsonCreator
    public ExecutionMetricSample(@JsonProperty("sample") MetricSample sample,
                                 @JsonProperty("eId") String eId,
                                 @JsonProperty("rnId") String rnId,
                                 @JsonProperty("planId") String planId,
                                 @JsonProperty("plan") String plan,
                                 @JsonProperty("canonicalPlanName") String canonicalPlanName,
                                 @JsonProperty("taskId") String taskId,
                                 @JsonProperty("schedule") String schedule,
                                 @JsonProperty("execution") String execution,
                                 @JsonProperty("agentUrl") String agentUrl,
                                 @JsonProperty("origin") String origin,
                                 @JsonProperty("attributes") Map<String, String> attributes,
                                 @JsonProperty("metricType") String metricType) {
        super(sample, metricType);
        this.eId = eId;
        this.rnId = rnId;
        this.planId = planId;
        this.plan = plan;
        this.canonicalPlanName = canonicalPlanName;
        this.taskId = taskId;
        this.schedule = schedule;
        this.execution = execution;
        this.agentUrl = agentUrl;
        this.origin = origin;
        this.attributes = attributes;
    }

    /**
     * Returns a merged label map combining the keyword-developer-defined labels on the metric
     * with the execution-context labels (execId, planId, agentUrl, etc.).
     * <p>
     * This is the full set of dimensions that handlers (e.g. time-series, Prometheus) should
     * use when writing the metric to their respective storage.
     * <p>
     * User-defined labels take precedence only for keys not already set by the context.
     * Context labels are set last so they cannot be overridden by user labels.
     */
    @JsonIgnore
    @Override
    public TreeMap<String, String> getEffectiveLabels() {
        TreeMap<String, String> labels = new TreeMap<>(sample.getLabels());
        putIfNotEmpty(labels, MetricsExecutionPlugin.ORIGIN, origin);
        if (attributes != null) {
            labels.putAll(attributes);
        }
        // Context labels are authoritative — set last so they cannot be overridden
        putIfNotEmpty(labels, MetricsExecutionPlugin.ATTRIBUTE_EXECUTION_ID, eId);
        putIfNotEmpty(labels, MetricsExecutionPlugin.PLAN_ID, planId);
        putIfNotEmpty(labels, MetricsExecutionPlugin.PLAN, plan);
        putIfNotEmpty(labels, MetricsExecutionPlugin.CANONICAL_PLAN_NAME, canonicalPlanName);
        putIfNotEmpty(labels, MetricsExecutionPlugin.AGENT_URL, agentUrl);
        putIfNotEmpty(labels, MetricsExecutionPlugin.TASK_ID, taskId);
        putIfNotEmpty(labels, MetricsExecutionPlugin.SCHEDULE, schedule);
        putIfNotEmpty(labels, MetricsExecutionPlugin.EXECUTION_DESCRIPTION, execution);
        return labels;
    }

    private void putIfNotEmpty(Map<String, String> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }
}
