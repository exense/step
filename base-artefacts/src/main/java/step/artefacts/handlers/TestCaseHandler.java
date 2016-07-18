package step.artefacts.handlers;

import step.artefacts.TestCase;
import step.artefacts.handlers.scheduler.SequentialArtefactScheduler;
import step.artefacts.reports.TestCaseReportNode;
import step.core.artefacts.handlers.ReportNodeAttributesManager;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;

public class TestCaseHandler extends ArtefactHandler<TestCase, ReportNode> {
	
	@Override
	public void createReportSkeleton_(ReportNode node, TestCase testArtefact) {
		addTestCaseNameToCustomAttributes(testArtefact);
		SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
		scheduler.createReportSkeleton_(node, testArtefact);
	}
	
	@Override
	public void execute_(ReportNode node, TestCase testArtefact) {
		addTestCaseNameToCustomAttributes(testArtefact);
		SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
		scheduler.execute_(node, testArtefact);
	}
	
	private void addTestCaseNameToCustomAttributes(TestCase testArtefact) {
		ReportNodeAttributesManager.addCustomAttribute("TestCase", testArtefact.getId().toString());
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, TestCase testArtefact) {
		return new TestCaseReportNode();
	}

}
