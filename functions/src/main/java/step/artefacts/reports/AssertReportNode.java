package step.artefacts.reports;

import step.core.artefacts.reports.ReportNode;

public class AssertReportNode extends ReportNode {

	String message;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
