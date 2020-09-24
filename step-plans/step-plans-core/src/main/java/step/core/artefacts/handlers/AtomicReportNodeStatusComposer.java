package step.core.artefacts.handlers;

import step.core.artefacts.reports.ReportNodeStatus;

public class AtomicReportNodeStatusComposer {

	protected ReportNodeStatus parentStatus;
	
	public AtomicReportNodeStatusComposer(ReportNodeStatus initialParentStatus) {
		super();
		this.parentStatus = initialParentStatus;
	}

	public AtomicReportNodeStatusComposer() {
		super();
	}

	public synchronized void addStatusAndRecompose(ReportNodeStatus reportNodeStatus) {
		if(parentStatus==null || reportNodeStatus.ordinal()<parentStatus.ordinal()) {
			parentStatus = reportNodeStatus;
		}
	}

	public ReportNodeStatus getParentStatus() {
		return parentStatus;
	}
}
