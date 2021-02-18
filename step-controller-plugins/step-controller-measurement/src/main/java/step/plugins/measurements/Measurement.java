package step.plugins.measurements;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Measurement extends HashMap<String, Object> {
	long begin;
	long value;
	String name = "";
	String type = "";
	String status = "";
	String execId = "";
	String taskId = "";
	String planId = "";
	Map<String, Object> customFields = new HashMap<>();

	public long getBegin() {
		return begin;
	}

	public void setBegin(long begin) {
		this.begin = begin;
		this.put(AbstractMeasurementPlugin.BEGIN, begin);
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
		this.put(AbstractMeasurementPlugin.VALUE, value);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		this.put(AbstractMeasurementPlugin.NAME, name);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
		this.put(AbstractMeasurementPlugin.TYPE, type);
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
		this.put(AbstractMeasurementPlugin.RN_STATUS, status);
	}

	public String getExecId() {
		return execId;
	}

	public void setExecId(String execId) {
		this.execId = execId;
		this.put(AbstractMeasurementPlugin.ATTRIBUTE_EXECUTION_ID, execId);
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
		this.put(AbstractMeasurementPlugin.TASK_ID, taskId);
	}

	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
		this.put(AbstractMeasurementPlugin.PLAN_ID, planId);
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

}
