package step.artefacts.handlers;

import step.artefacts.StreamingArtefact;
import step.artefacts.ArtefactQueue.WorkItem;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;

public class StreamingArtefactHandler extends ArtefactHandler<StreamingArtefact, ReportNode> {

	@Override
	protected void createReportSkeleton_(ReportNode parentNode, StreamingArtefact testArtefact) {
		
	}

	@Override
	protected void execute_(ReportNode reportNode, StreamingArtefact artefact) throws Exception {
		WorkItem workItem;
		while((workItem = artefact.takeFromQueue())!=null) {
			ReportNode resultReportNode = delegateExecute(workItem.getArtefact(), reportNode);
			workItem.complete(resultReportNode);
		}
	}

	@Override
	protected ReportNode createReportNode_(ReportNode parentReportNode, StreamingArtefact artefact) {
		return new ReportNode();
	}

}
