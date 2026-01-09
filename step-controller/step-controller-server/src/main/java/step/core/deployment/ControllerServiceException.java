package step.core.deployment;

public class ControllerServiceException extends RuntimeException {

	private final int httpErrorCode;
	private final String errorName;
	private final String errorMessage;
	private final Object errorDetails;
    private boolean technicalError = true;

	public ControllerServiceException(String errorMessage) {
		super(errorMessage);
		this.httpErrorCode = 500;
		this.errorName = null;
		this.errorMessage = errorMessage;
		this.errorDetails = null;
	}
	
	public ControllerServiceException(int httpErrorCode, String errorMessage) {
		super(errorMessage);
		this.httpErrorCode = httpErrorCode;
		this.errorName = null;
		this.errorMessage = errorMessage;
		this.errorDetails = null;
	}

	public ControllerServiceException(int httpErrorCode, String errorName, String errorMessage) {
		super(errorMessage);
		this.httpErrorCode = httpErrorCode;
		this.errorName = errorName;
		this.errorMessage = errorMessage;
		this.errorDetails = null;
	}

	public ControllerServiceException(int httpErrorCode, String errorName, String errorMessage, Object errorDetails) {
		super(errorMessage);
		this.httpErrorCode = httpErrorCode;
		this.errorName = errorName;
		this.errorMessage = errorMessage;
		this.errorDetails = errorDetails;
	}

	public ControllerServiceException(String errorMessage, Throwable cause) {
		super(errorMessage, cause);
		this.httpErrorCode = 500;
		this.errorName = null;
		this.errorMessage = errorMessage;
		this.errorDetails = null;
	}

	public ControllerServiceException(int httpErrorCode, String errorMessage, Throwable cause) {
		super(errorMessage, cause);
		this.httpErrorCode = httpErrorCode;
		this.errorName = null;
		this.errorMessage = errorMessage;
		this.errorDetails = null;
	}

	public ControllerServiceException(int httpErrorCode, String errorName, String errorMessage, Throwable cause) {
		super(errorMessage, cause);
		this.httpErrorCode = httpErrorCode;
		this.errorName = errorName;
		this.errorMessage = errorMessage;
		this.errorDetails = null;
	}

	public int getHttpErrorCode() {
		return httpErrorCode;
	}

	public String getErrorName() {
		return errorName;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Object getErrorDetails() {
		return errorDetails;
	}

	@Override
	public String toString() {
		return "ControllerServiceException{" +
				"httpErrorCode=" + httpErrorCode +
				", errorName='" + errorName + '\'' +
				", errorMessage='" + errorMessage + '\'' +
				", errorDetails=" + errorDetails +
				'}';
	}

	public boolean isTechnicalError() {
		return technicalError;
	}

	public void setTechnicalError(boolean technicalError) {
		this.technicalError = technicalError;
	}
}
