package step.artefacts;

import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;

public class CheckArtefactHandler extends ArtefactHandler<CheckArtefact, ReportNode> {

	@Override
	protected void createReportSkeleton_(ReportNode parentNode, CheckArtefact testArtefact) {
	}

	@Override
	protected void execute_(ReportNode node, CheckArtefact testArtefact) {
		testArtefact.getExecutionRunnable().run();
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode,
			CheckArtefact testArtefact) {
		return new ReportNode();
	}

}
