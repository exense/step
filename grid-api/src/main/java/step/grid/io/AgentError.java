package step.grid.io;

import java.util.Map;

public class AgentError {

	private AgentErrorCode errorCode;
	
	private Map<AgentErrorCode.Details, String> errorDetails;
	
	public AgentError() {
		super();
	}

	public AgentError(AgentErrorCode errorCode) {
		super();
		this.errorCode = errorCode;
	}

	public AgentError(AgentErrorCode errorCode, Map<AgentErrorCode.Details, String> errorDetails) {
		super();
		this.errorCode = errorCode;
		this.errorDetails = errorDetails;
	}

	public AgentErrorCode getErrorCode() {
		return errorCode;
	}

	public Map<AgentErrorCode.Details, String> getErrorDetails() {
		return errorDetails;
	}

	public void setErrorCode(AgentErrorCode errorCode) {
		this.errorCode = errorCode;
	}

	public void setErrorDetails(Map<AgentErrorCode.Details, String> errorDetails) {
		this.errorDetails = errorDetails;
	}
}
