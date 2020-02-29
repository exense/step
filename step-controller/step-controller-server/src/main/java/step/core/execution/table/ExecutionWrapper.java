package step.core.execution.table;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.model.Execution;

public class ExecutionWrapper extends Execution {
	
	private ReportNode rootReportNode;
	
	private Object executionSummary;

	public ReportNode getRootReportNode() {
		return rootReportNode;
	}

	public void setRootReportNode(ReportNode rootReportNode) {
		this.rootReportNode = rootReportNode;
	}

	public Object getExecutionSummary() {
		return executionSummary;
	}

	public void setExecutionSummary(Object executionSummary) {
		this.executionSummary = executionSummary;
	}
}