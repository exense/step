package step.core.execution;

public class ExecutionEngineContext extends AbstractExecutionEngineContext {

	private final OperationMode operationMode;
	
	public ExecutionEngineContext(OperationMode operationMode) {
		super();
		this.operationMode = operationMode;
	}

	public OperationMode getOperationMode() {
		return operationMode;
	}
}