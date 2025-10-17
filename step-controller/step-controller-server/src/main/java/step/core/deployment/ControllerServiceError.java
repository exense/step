package step.core.deployment;

public class ControllerServiceError {

	private String errorName;
	private String errorMessage;
	private Object errorDetails;

	public String getErrorName() {
		return errorName;
	}

	public void setErrorName(String errorName) {
		this.errorName = errorName;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Object getErrorDetails() {
		return errorDetails;
	}

	public void setErrorDetails(Object errorDetails) {
		this.errorDetails = errorDetails;
	}
}