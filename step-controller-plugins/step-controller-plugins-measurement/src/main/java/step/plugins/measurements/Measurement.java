package step.plugins.measurements;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Measurement extends HashMap<String, Object> {
	Map<String, Object> customFields = new HashMap<>();

	Map<String, String> additionalAttributes = new TreeMap<>();

	public long getBegin() {
		return (long) this.get(MeasurementPlugin.BEGIN);
	}

	public void setBegin(long begin) {
		this.put(MeasurementPlugin.BEGIN, begin);
	}

	public long getValue() {
		return (long) this.get(MeasurementPlugin.VALUE);
	}

	public void setValue(long value) {
		this.put(MeasurementPlugin.VALUE, value);
	}

	public String getName() {
		return (String) this.get(MeasurementPlugin.NAME);
	}

	public void setName(String name) {
		this.put(MeasurementPlugin.NAME, name);
	}

	public String getType() {
		return (String) this.get(MeasurementPlugin.TYPE);
	}

	public void setType(String type) {
		this.put(MeasurementPlugin.TYPE, type);
	}

	public String getStatus() {
		return (String) this.get(MeasurementPlugin.RN_STATUS);
	}

	public void setStatus(String status) {
		this.put(MeasurementPlugin.RN_STATUS, status);
	}

	public String getExecId() {
		return (String) this.get(MeasurementPlugin.ATTRIBUTE_EXECUTION_ID);
	}

	public void setExecId(String execId) {
		this.put(MeasurementPlugin.ATTRIBUTE_EXECUTION_ID, execId);
	}

	public String getExecution() {
		return (String) this.get(MeasurementPlugin.EXECUTION_DESCRIPTION);
	}

	public void setExecution(String execution) {
		this.put(MeasurementPlugin.EXECUTION_DESCRIPTION, execution);
	}

	public String getTaskId() {
		return (String) this.get(MeasurementPlugin.TASK_ID);
	}

	public void setTaskId(String taskId) {
		this.put(MeasurementPlugin.TASK_ID, taskId);
	}

	public String getSchedule() {
		return (String) this.get(MeasurementPlugin.SCHEDULE);
	}

	public void setSchedule(String schedule) {
		this.put(MeasurementPlugin.SCHEDULE, schedule);
	}

	public String getPlanId() {
		return (String) this.get(MeasurementPlugin.PLAN_ID);
	}

	public void setPlanId(String planId) {
		this.put(MeasurementPlugin.PLAN_ID, planId);
	}

	public String getPlan() {
		return (String) this.get(MeasurementPlugin.PLAN);
	}

	public void setPlan(String plan) {
		this.put(MeasurementPlugin.PLAN, plan);
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
