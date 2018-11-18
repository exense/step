package step.functions.execution;

@SuppressWarnings("serial")
public class FunctionExecutionServiceException extends Exception {

	public FunctionExecutionServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public FunctionExecutionServiceException(String message) {
		super(message);
	}

	public FunctionExecutionServiceException(Throwable cause) {
		super(cause);
	}

}
