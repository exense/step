package step.client;

public class ControllerClientException extends RuntimeException {

	private static final long serialVersionUID = -4616893921997561349L;

	public ControllerClientException() {
		super();
	}

	public ControllerClientException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ControllerClientException(String message, Throwable cause) {
		super(message, cause);
	}

	public ControllerClientException(String message) {
		super(message);
	}

	public ControllerClientException(Throwable cause) {
		super(cause);
	}

}
