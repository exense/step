package step.artefacts.handlers;

import step.artefacts.Check;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class CheckHandler extends ArtefactHandler<Check, ReportNode> {
	
	@Override
	protected void createReportSkeleton_(ReportNode parentNode, Check testArtefact) {

	}

	@Override
	protected void execute_(ReportNode node, Check testArtefact) {
		String expression = testArtefact.getExpression();
		if(expression!=null) {
			try {
				Boolean result = Boolean.parseBoolean(expression);
				if(result) {
					node.setStatus(ReportNodeStatus.PASSED);
				} else {
					node.setStatus(ReportNodeStatus.FAILED);
				}
			} catch (Exception e) {
				node.setError("The check expression didn't return a boolean");
				node.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
			} 
		} else {
			node.setError("The check expression is null");
			node.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
		}
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Check testArtefact) {
		return new ReportNode();
	}
}
