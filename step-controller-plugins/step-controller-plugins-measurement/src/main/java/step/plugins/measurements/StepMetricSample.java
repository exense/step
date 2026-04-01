package step.plugins.measurements;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import step.core.accessors.AbstractOrganizableObject;
import step.core.metrics.MetricSample;

import java.util.Map;
import java.util.TreeMap;

/**
 * Controller-side representation of a {@link MetricSample}, enriched with
 * execution context (execution ID, plan, agent URL, schedule, etc.) in the same way
 * as {@link Measurement} is enriched for legacy {@code Measure} objects.
 * <p>
 * Unlike {@link Measurement}, this is a typed flat POJO rather than a {@link java.util.HashMap}
 * subclass. Handlers receive lists of these via {@link SamplesHandler#processMetrics}.
 */
public class StepMetricSample extends AbstractOrganizableObject {

    public final MetricSample sample;
    public final String eId;
    public final String rnId;
    public final String planId;
    public final String plan;
    public final String taskId;
    public final String schedule;
    public final String execution;
    public final String agentUrl;
    public final String origin;

    @JsonCreator
    public StepMetricSample(@JsonProperty("sample") MetricSample sample,
                            @JsonProperty("eId") String eId,
                            @JsonProperty("rnId") String rnId,
                            @JsonProperty("planId") String planId,
                            @JsonProperty("plan") String plan,
                            @JsonProperty("taskId") String taskId,
                            @JsonProperty("schedule") String schedule,
                            @JsonProperty("execution") String execution,
                            @JsonProperty("agentUrl") String agentUrl,
                            @JsonProperty("origin") String origin,
                            @JsonProperty("attributes") Map<String, String> attributes) {
        this.sample = sample;
        this.eId = eId;
        this.rnId = rnId;
        this.planId = planId;
        this.plan = plan;
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
    public Map<String, String> getEffectiveLabels() {
        TreeMap<String, String> labels = new TreeMap<>(sample.getLabels());
        if (origin != null) {
            labels.put(SamplesExecutionPlugin.ORIGIN, origin);
        }
        if (attributes != null) {
            labels.putAll(attributes);
        }
        // Context labels are authoritative — set last so they cannot be overridden
        labels.put(SamplesExecutionPlugin.ATTRIBUTE_EXECUTION_ID, eId);
        labels.put(SamplesExecutionPlugin.PLAN_ID, planId);
        labels.put(SamplesExecutionPlugin.PLAN, plan);
        if (agentUrl != null) {
            labels.put(SamplesExecutionPlugin.AGENT_URL, agentUrl);
        }
        if (taskId != null && !taskId.isEmpty()) {
            labels.put(SamplesExecutionPlugin.TASK_ID, taskId);
        }
        if (schedule != null && !schedule.isEmpty()) {
            labels.put(SamplesExecutionPlugin.SCHEDULE, schedule);
        }
        if (execution != null && !execution.isEmpty()) {
            labels.put(SamplesExecutionPlugin.EXECUTION_DESCRIPTION, execution);
        }
        return labels;
    }
}
