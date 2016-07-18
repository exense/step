package step.artefacts.handlers;

import step.artefacts.handlers.scheduler.SequentialArtefactScheduler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;

public class SequenceHandler extends ArtefactHandler<AbstractArtefact, ReportNode> {
	
	@Override
	public void createReportSkeleton_(ReportNode node, AbstractArtefact testArtefact) {
		SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
		scheduler.createReportSkeleton_(node, testArtefact);
	}
	
	@Override
	public void execute_(ReportNode node, AbstractArtefact testArtefact) {
		SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
		scheduler.execute_(node, testArtefact);
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, AbstractArtefact testArtefact) {
		return new ReportNode();
	}

}
