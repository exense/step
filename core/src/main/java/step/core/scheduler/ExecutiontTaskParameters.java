package step.core.scheduler;

import org.jongo.marshall.jackson.oid.ObjectId;

import step.core.execution.model.ExecutionParameters;

public class ExecutiontTaskParameters {
	
	@ObjectId
	public String _id;
	
	public String name;
	
	public ExecutionParameters executionsParameters;
	
	public String cronExpression;
	
	public boolean active;

	public ExecutiontTaskParameters() {
		super();
	}

	public ExecutiontTaskParameters(
			ExecutionParameters executionsParameters, String cronExpr) {
		super();
		this.executionsParameters = executionsParameters;
		this.cronExpression = cronExpr;
		this.active = true;
	}
	
	public String getId() {
		return _id;
	}
	
	public void setId(String id) {
		_id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ExecutionParameters getExecutionsParameters() {
		return executionsParameters;
	}

	public String getCronExpression() {
		return cronExpression;
	}

	public void setExecutionsParameters(ExecutionParameters executionsParameters) {
		this.executionsParameters = executionsParameters;
	}

	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	

}
