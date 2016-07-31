package step.artefacts.handlers;

import step.artefacts.Set;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class SetHandler extends ArtefactHandler<Set, ReportNode> {
	
	@Override
	protected void createReportSkeleton_(ReportNode parentNode, Set testArtefact) {

	}

	@Override
	protected void execute_(ReportNode node, Set testArtefact) {
		node.setStatus(ReportNodeStatus.PASSED);
		ReportNode parentNode = context.getReportNodeCache().get(node.getParentID().toString());
		context.getVariablesManager().putVariable(parentNode, testArtefact.getKey(), testArtefact.getValue());
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Set testArtefact) {
		return new ReportNode();
	}
}
