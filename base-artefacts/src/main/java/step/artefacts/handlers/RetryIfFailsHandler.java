package step.artefacts.handlers;

import step.artefacts.RetryIfFails;
import step.artefacts.Sequence;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class RetryIfFailsHandler extends ArtefactHandler<RetryIfFails, ReportNode> {

	@Override
	protected void createReportSkeleton_(ReportNode parentNode, RetryIfFails testArtefact) {
		ArtefactAccessor artefactAccessor = context.getGlobalContext().getArtefactAccessor();
		Sequence iterationTestCase = artefactAccessor.createWorkArtefact(Sequence.class, testArtefact, "Iteration"+1, true);
		delegateCreateReportSkeleton(iterationTestCase, parentNode);
	}

	@Override
	protected void execute_(ReportNode node, RetryIfFails testArtefact) {
		boolean success = false;
		
		for(int count = 1; count<=testArtefact.getMaxRetries();count++) {
			ArtefactAccessor artefactAccessor = context.getGlobalContext().getArtefactAccessor();
			Sequence iterationTestCase = artefactAccessor.createWorkArtefact(Sequence.class, testArtefact, "Iteration"+count, true);
			
			ReportNode iterationReportNode = delegateExecute(iterationTestCase, node);
			
			if(iterationReportNode.getStatus()==ReportNodeStatus.PASSED) {
				success = true;
			}
			
			if(iterationReportNode.getStatus()==ReportNodeStatus.PASSED || context.isInterrupted()) {
				break;
			}
			
			try {
				Thread.sleep(testArtefact.getGracePeriod());
			} catch (InterruptedException e) {}
		}
		
		node.setStatus(success?ReportNodeStatus.PASSED:ReportNodeStatus.FAILED);
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, RetryIfFails testArtefact) {
		return new ReportNode();
	}

}
