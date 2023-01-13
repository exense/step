package step.core.deployment;

@SuppressWarnings("serial")
public class ControllerServiceException extends RuntimeException {

	private int httpErrorCode;
	private String errorName;
	private String errorMessage;

	public ControllerServiceException(String errorMessage) {
		super(errorMessage);
		this.httpErrorCode = 500;
		this.errorName = null;
		this.errorMessage = errorMessage;
	}
	
	public ControllerServiceException(int httpErrorCode, String errorMessage) {
		super(errorMessage);
		this.httpErrorCode = httpErrorCode;
		this.errorName = null;
		this.errorMessage = errorMessage;
	}

	public ControllerServiceException(int httpErrorCode, String errorName, String errorMessage) {
		super(errorMessage);
		this.httpErrorCode = httpErrorCode;
		this.errorName = errorName;
		this.errorMessage = errorMessage;
	}

	public ControllerServiceException(String errorMessage, Throwable cause) {
		super(errorMessage, cause);
		this.httpErrorCode = 500;
		this.errorName = null;
		this.errorMessage = errorMessage;
	}

	public ControllerServiceException(int httpErrorCode, String errorMessage, Throwable cause) {
		super(errorMessage, cause);
		this.httpErrorCode = httpErrorCode;
		this.errorName = null;
		this.errorMessage = errorMessage;
	}

	public ControllerServiceException(int httpErrorCode, String errorName, String errorMessage, Throwable cause) {
		super(errorMessage, cause);
		this.httpErrorCode = httpErrorCode;
		this.errorName = errorName;
		this.errorMessage = errorMessage;
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
}
