package step.core.repositories;

import step.core.artefacts.reports.ReportNodeStatus;

public class TestRunStatus {
	
	String testplanName;
	
	ReportNodeStatus status;

	public TestRunStatus() {
		super();
	}

	public TestRunStatus(String testplanName, ReportNodeStatus status) {
		super();
		this.testplanName = testplanName;
		this.status = status;
	}

	public String getTestplanName() {
		return testplanName;
	}

	public void setTestplanName(String testplanName) {
		this.testplanName = testplanName;
	}

	public ReportNodeStatus getStatus() {
		return status;
	}

	public void setStatus(ReportNodeStatus status) {
		this.status = status;
	}

}
