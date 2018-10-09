package step.artefacts.handlers;

import step.grid.TokenWrapperOwner;

public class CallFunctionTokenWrapperOwner implements TokenWrapperOwner {

	private String reportNodeId;
	private String executionId;
	private String executionDescription;
	
	public CallFunctionTokenWrapperOwner(String reportNodeId, String executionId, String executionDescription) {
		super();
		this.reportNodeId = reportNodeId;
		this.executionId = executionId;
		this.executionDescription = executionDescription;
	}

	public String getReportNodeId() {
		return reportNodeId;
	}

	public String getExecutionId() {
		return executionId;
	}

	public String getExecutionDescription() {
		return executionDescription;
	}
	
	
}
