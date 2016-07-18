package step.artefacts.reports;

import step.core.artefacts.reports.ReportNode;

public class ForBlockReportNode extends ReportNode {

	private int errorCount;
	
	private int count;

	public int getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(int errorCount) {
		this.errorCount = errorCount;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	
}
