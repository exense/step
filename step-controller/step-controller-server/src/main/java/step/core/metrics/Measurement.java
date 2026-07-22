package step.core.metrics;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Measurement extends HashMap<String, Object> {
    Map<String, Object> customFields = new HashMap<>();

    Map<String, String> additionalAttributes = new TreeMap<>();

    /**
     * Keyword-author-defined custom data keys (from the originating {@code Measure}'s data map).
     * These are the labels considered "user-defined" and therefore subject to cardinality control
     * by the time-series handler. Transient and ignored during (de)serialization so it never leaks
     * into the persisted RAW measurement.
     */
    @JsonIgnore
    private transient Set<String> customMetricLabelKeys;

    public long getBegin() {
        return (long) this.get(MetricsExecutionPlugin.BEGIN);
    }

    public void setBegin(long begin) {
        this.put(MetricsExecutionPlugin.BEGIN, begin);
    }

    public long getValue() {
        return (long) this.get(MetricsExecutionPlugin.VALUE);
    }

    public void setValue(long value) {
        this.put(MetricsExecutionPlugin.VALUE, value);
    }

    public String getName() {
        return (String) this.get(MetricsExecutionPlugin.NAME);
    }

    public void setName(String name) {
        this.put(MetricsExecutionPlugin.NAME, name);
    }

    public String getType() {
        return (String) this.get(MetricsExecutionPlugin.TYPE);
    }

    public void setType(String type) {
        this.put(MetricsExecutionPlugin.TYPE, type);
    }

    public String getStatus() {
        return (String) this.get(MetricsExecutionPlugin.RN_STATUS);
    }

    public void setStatus(String status) {
        this.put(MetricsExecutionPlugin.RN_STATUS, status);
    }

    public String getExecId() {
        return (String) this.get(MetricsExecutionPlugin.ATTRIBUTE_EXECUTION_ID);
    }

    public void setExecId(String execId) {
        this.put(MetricsExecutionPlugin.ATTRIBUTE_EXECUTION_ID, execId);
    }

    public String getExecution() {
        return (String) this.get(MetricsExecutionPlugin.EXECUTION_DESCRIPTION);
    }

    public void setExecution(String execution) {
        this.put(MetricsExecutionPlugin.EXECUTION_DESCRIPTION, execution);
    }

    public String getTaskId() {
        return (String) this.get(MetricsExecutionPlugin.TASK_ID);
    }

    public void setTaskId(String taskId) {
        this.put(MetricsExecutionPlugin.TASK_ID, taskId);
    }

    public String getSchedule() {
        return (String) this.get(MetricsExecutionPlugin.SCHEDULE);
    }

    public void setSchedule(String schedule) {
        this.put(MetricsExecutionPlugin.SCHEDULE, schedule);
    }

    public String getPlanId() {
        return (String) this.get(MetricsExecutionPlugin.PLAN_ID);
    }

    public void setPlanId(String planId) {
        this.put(MetricsExecutionPlugin.PLAN_ID, planId);
    }

    public String getPlan() {
        return (String) this.get(MetricsExecutionPlugin.PLAN);
    }

    public void setPlan(String plan) {
        this.put(MetricsExecutionPlugin.PLAN, plan);
    }

    public String getCanonicalPlanName() {
        return (String) this.get(MetricsExecutionPlugin.CANONICAL_PLAN_NAME);
    }

    public void setCanonicalPlanName(String canonicalPlanName) {
        this.put(MetricsExecutionPlugin.CANONICAL_PLAN_NAME, canonicalPlanName);
    }

    public Map<String, Object> getCustomFields() {
        return Collections.unmodifiableMap(customFields);
    }

    public void addCustomField(String key, Object value) {
        this.customFields.put(key, value);
        this.put(key, value);
    }

    public void addCustomFields(Map<String, ?> fields) {
        this.customFields.putAll(fields);
        this.putAll(fields);
    }

    public Map<String, String> getAdditionalAttributes() {
        return additionalAttributes;
    }

    public void addAdditionalAttributes(TreeMap<String, String> additionalAttributes) {
        this.additionalAttributes.putAll(additionalAttributes);
        this.putAll(additionalAttributes);
    }

    @JsonIgnore
    public Set<String> getCustomMetricLabelKeys() {
        return customMetricLabelKeys;
    }

    @JsonIgnore
    public void setCustomMetricLabelKeys(Set<String> customMetricLabelKeys) {
        this.customMetricLabelKeys = customMetricLabelKeys;
    }


}
