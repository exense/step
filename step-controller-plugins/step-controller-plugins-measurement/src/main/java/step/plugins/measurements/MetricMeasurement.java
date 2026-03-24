package step.plugins.measurements;

import step.core.accessors.AbstractOrganizableObject;
import step.core.metrics.MetricSnapshot;
import step.core.metrics.MetricType;

import java.util.Map;
import java.util.TreeMap;

/**
 * Controller-side representation of a {@link MetricSnapshot}, enriched with
 * execution context (execution ID, plan, agent URL, schedule, etc.) in the same way
 * as {@link Measurement} is enriched for legacy {@code Measure} objects.
 * <p>
 * Unlike {@link Measurement}, this is a typed flat POJO rather than a {@link java.util.HashMap}
 * subclass. Handlers receive lists of these via {@link MeasurementHandler#processMetrics}.
 */
public class MetricMeasurement extends AbstractOrganizableObject {

    private long snapshotTimestamp;
    private String name;
    private Map<String, String> labels;
    private MetricType type;
    private final String eId;
    private final String rnId;
    private final String planId;
    private final String plan;
    private final String taskId;
    private final String schedule;
    private final String execution;
    private final String agentUrl;
    private final Map<String, String> functionAttributes;
    private final String status;
    private final Map<String, String> additionalAttributes;

    public MetricMeasurement(MetricSnapshot metric,
                             String eId,
                             String rnId,
                             String planId,
                             String plan,
                             String taskId,
                             String schedule,
                             String execution,
                             String agentUrl,
                             Map<String, String> functionAttributes,
                             String status,
                             Map<String, String> additionalAttributes) {
        this.metric = metric;
        this.eId = eId;
        this.rnId = rnId;
        this.planId = planId;
        this.plan = plan;
        this.taskId = taskId;
        this.schedule = schedule;
        this.execution = execution;
        this.agentUrl = agentUrl;
        this.functionAttributes = functionAttributes;
        this.status = status;
        this.additionalAttributes = additionalAttributes;
    }

    /**
     * Returns the metric snapshot. The concrete subtype ({@code CounterSnapshot},
     * {@code SampledSnapshot}) can be determined via {@link MetricSnapshot#getType()}.
     */
    public MetricSnapshot getMetric() {
        return metric;
    }

    public String getEId() {
        return eId;
    }

    public String getRnId() {
        return rnId;
    }

    public String getPlanId() {
        return planId;
    }

    public String getPlan() {
        return plan;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getExecution() {
        return execution;
    }

    public String getAgentUrl() {
        return agentUrl;
    }

    public Map<String, String> getFunctionAttributes() {
        return functionAttributes;
    }

    public String getStatus() {
        return status;
    }

    public Map<String, String> getAdditionalAttributes() {
        return additionalAttributes;
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
    public Map<String, String> getEffectiveLabels() {
        TreeMap<String, String> labels = new TreeMap<>(metric.getLabels());
        if (functionAttributes != null) {
            labels.putAll(functionAttributes);
        }
        if (additionalAttributes != null) {
            labels.putAll(additionalAttributes);
        }
        // Context labels are authoritative — set last so they cannot be overridden
        labels.put(MeasurementPlugin.ATTRIBUTE_EXECUTION_ID, eId);
        labels.put(MeasurementPlugin.PLAN_ID, planId);
        labels.put(MeasurementPlugin.PLAN, plan);
        if (agentUrl != null) {
            labels.put(MeasurementPlugin.AGENT_URL, agentUrl);
        }
        if (taskId != null && !taskId.isEmpty()) {
            labels.put(MeasurementPlugin.TASK_ID, taskId);
        }
        if (schedule != null && !schedule.isEmpty()) {
            labels.put(MeasurementPlugin.SCHEDULE, schedule);
        }
        if (execution != null && !execution.isEmpty()) {
            labels.put(MeasurementPlugin.EXECUTION_DESCRIPTION, execution);
        }
        return labels;
    }
}
