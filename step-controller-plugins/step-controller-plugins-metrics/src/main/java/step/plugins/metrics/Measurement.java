package step.plugins.metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Measurement extends HashMap<String, Object> {
    Map<String, Object> customFields = new HashMap<>();

    Map<String, String> additionalAttributes = new TreeMap<>();

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


}
