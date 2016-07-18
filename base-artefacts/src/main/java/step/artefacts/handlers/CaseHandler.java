package step.artefacts.handlers;

import step.artefacts.Case;
import step.artefacts.handlers.scheduler.SequentialArtefactScheduler;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;

public class CaseHandler extends ArtefactHandler<Case, ReportNode> {

	@Override
	protected void createReportSkeleton_(ReportNode parentNode,
			Case testArtefact) {
		SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
		scheduler.createReportSkeleton_(parentNode, testArtefact);
	}

	@Override
	protected void execute_(ReportNode node, Case testArtefact) {
		SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
		scheduler.execute_(node, testArtefact);
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Case testArtefact) {
		return new ReportNode();
	}

}
