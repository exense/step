package step.core.execution.type;

public abstract class ExecutionType {
	
	protected String name;
	
	public abstract Object getExecutionSummary(String executionId);

	public ExecutionType(String name) {
		super();	
		this.name = name;
	}

	protected String getName() {
		return name;
	}
}
