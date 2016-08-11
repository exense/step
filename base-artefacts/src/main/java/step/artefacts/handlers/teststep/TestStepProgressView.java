package step.artefacts.handlers.teststep;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.reports.ReportNode;
import step.plugins.progressunit.ProgressView;

public class TestStepProgressView implements ProgressView {

	volatile int max = 0;
		
	@Override
	public void skeletonReportNodeCreated(ReportNode node) {
		if(node instanceof CallFunctionReportNode) {
			max++;
		}
	}

	@Override
	public void reportNodeExecuted(ReportNode node) {
	}
	
	@Override
	public int getMaxProgress() {
		return max;
	}

}
