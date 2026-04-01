package step.plugins.measurements;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Measurement extends HashMap<String, Object> {
    Map<String, Object> customFields = new HashMap<>();

    Map<String, String> additionalAttributes = new TreeMap<>();

    public long getBegin() {
        return (long) this.get(SamplesExecutionPlugin.BEGIN);
    }

    public void setBegin(long begin) {
        this.put(SamplesExecutionPlugin.BEGIN, begin);
    }

    public long getValue() {
        return (long) this.get(SamplesExecutionPlugin.VALUE);
    }

    public void setValue(long value) {
        this.put(SamplesExecutionPlugin.VALUE, value);
    }

    public String getName() {
        return (String) this.get(SamplesExecutionPlugin.NAME);
    }

    public void setName(String name) {
        this.put(SamplesExecutionPlugin.NAME, name);
    }

    public String getType() {
        return (String) this.get(SamplesExecutionPlugin.TYPE);
    }

    public void setType(String type) {
        this.put(SamplesExecutionPlugin.TYPE, type);
    }

    public String getStatus() {
        return (String) this.get(SamplesExecutionPlugin.RN_STATUS);
    }

    public void setStatus(String status) {
        this.put(SamplesExecutionPlugin.RN_STATUS, status);
    }

    public String getExecId() {
        return (String) this.get(SamplesExecutionPlugin.ATTRIBUTE_EXECUTION_ID);
    }

    public void setExecId(String execId) {
        this.put(SamplesExecutionPlugin.ATTRIBUTE_EXECUTION_ID, execId);
    }

    public String getExecution() {
        return (String) this.get(SamplesExecutionPlugin.EXECUTION_DESCRIPTION);
    }

    public void setExecution(String execution) {
        this.put(SamplesExecutionPlugin.EXECUTION_DESCRIPTION, execution);
    }

    public String getTaskId() {
        return (String) this.get(SamplesExecutionPlugin.TASK_ID);
    }

    public void setTaskId(String taskId) {
        this.put(SamplesExecutionPlugin.TASK_ID, taskId);
    }

    public String getSchedule() {
        return (String) this.get(SamplesExecutionPlugin.SCHEDULE);
    }

    public void setSchedule(String schedule) {
        this.put(SamplesExecutionPlugin.SCHEDULE, schedule);
    }

    public String getPlanId() {
        return (String) this.get(SamplesExecutionPlugin.PLAN_ID);
    }

    public void setPlanId(String planId) {
        this.put(SamplesExecutionPlugin.PLAN_ID, planId);
    }

    public String getPlan() {
        return (String) this.get(SamplesExecutionPlugin.PLAN);
    }

    public void setPlan(String plan) {
        this.put(SamplesExecutionPlugin.PLAN, plan);
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
