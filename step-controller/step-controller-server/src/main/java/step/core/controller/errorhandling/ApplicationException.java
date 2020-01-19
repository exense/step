package step.core.controller.errorhandling;

import java.util.Map;

@SuppressWarnings("serial")
public class ApplicationException extends RuntimeException {

	private int errorCode;
	private String errorMessage;
	private Map<String, String> data;

	public ApplicationException(int errorCode, String errorMessage, Map<String, String> data) {
		super();
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.data = data;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Map<String, String> getData() {
		return data;
	}
}
