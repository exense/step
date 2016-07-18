package step.core.scheduler;

import org.jongo.marshall.jackson.oid.ObjectId;

import step.core.execution.model.ExecutionParameters;

public class ExecutiontTaskParameters {
	
	@ObjectId
	public String _id;
	
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

	public ExecutionParameters getExecutionsParameters() {
		return executionsParameters;
	}

	public String getCronExpr() {
		return cronExpression;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	

}
