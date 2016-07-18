package step.core.execution;

import step.core.artefacts.reports.ReportNode;

public class ReportNodeTree {
	
	final ExecutionContext context;
	
	final ReportNodeCache cache;

	public ReportNodeTree(ExecutionContext context) {
		super();
		this.context = context;
		this.cache = context.getReportNodeCache();
	}

	public ReportNode getRoot() {
		return context.getReport();
	}
	
	public ReportNode getParent(ReportNode node) {
		return cache.get(node.getParentID().toString());
	}
}
