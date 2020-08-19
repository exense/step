package step.core.execution;

public class ExecutionEngineException extends RuntimeException {

	public ExecutionEngineException(Exception e) {
		super(e);
	}

	public ExecutionEngineException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExecutionEngineException(String message) {
		super(message);
	}

}
