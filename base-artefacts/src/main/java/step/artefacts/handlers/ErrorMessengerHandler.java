package step.artefacts.handlers;

import step.artefacts.ErrorMessenger;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class ErrorMessengerHandler extends ArtefactHandler<ErrorMessenger, ReportNode> {

	@Override
	public void createReportSkeleton_(ReportNode node, ErrorMessenger testArtefact) {
		execute_((ReportNode) node, testArtefact);
	}

	@Override
	public void execute_(ReportNode node, ErrorMessenger testArtefact) {
		for(String errorMsg:testArtefact.getErrorMessages()) {
			node.addError(errorMsg);
		}
		node.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, ErrorMessenger testArtefact) {
		return new ReportNode();
	}

}
