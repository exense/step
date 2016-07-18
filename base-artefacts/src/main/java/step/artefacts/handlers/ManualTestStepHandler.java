package step.artefacts.handlers;

import step.artefacts.ManualTestStep;
import step.artefacts.reports.TestStepReportNode;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class ManualTestStepHandler extends ArtefactHandler<ManualTestStep, TestStepReportNode> {
	
	@Override
	public void createReportSkeleton_(TestStepReportNode node, ManualTestStep testArtefact) {
	}

	@Override
	protected void execute_(TestStepReportNode node, ManualTestStep testArtefact) {
		node.setInput(testArtefact.getDescription());
		if(!testArtefact.isComment()) {
			node.setStatus(ReportNodeStatus.NOT_COMPLETED);
		} else {
			node.setStatus(ReportNodeStatus.PASSED);
		}
	}

	@Override
	public TestStepReportNode createReportNode_(ReportNode parentNode, ManualTestStep testArtefact) {
		return new TestStepReportNode();
	}


}
