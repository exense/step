package step.core.execution;

public class ExecutionEngineContext extends AbstractExecutionEngineContext {

	private final OperationMode operationMode;
	
	public ExecutionEngineContext(OperationMode operationMode) {
		this(operationMode, null);
	}
	
	public ExecutionEngineContext(OperationMode operationMode, AbstractExecutionEngineContext parentContext) {
		super(parentContext);
		this.operationMode = operationMode;
	}

	public OperationMode getOperationMode() {
		return operationMode;
	}
}