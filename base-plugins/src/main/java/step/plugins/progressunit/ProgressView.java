package step.plugins.progressunit;

import step.core.artefacts.reports.ReportNode;

public interface ProgressView {

	public void skeletonReportNodeCreated(ReportNode node);
	
	public void reportNodeExecuted(ReportNode node);
	
	public int getMaxProgress();
}
