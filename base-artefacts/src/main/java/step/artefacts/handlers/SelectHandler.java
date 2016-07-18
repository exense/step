package step.artefacts.handlers;

import step.artefacts.Case;
import step.artefacts.Select;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactAttributesHandler;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class SelectHandler extends ArtefactHandler<Select, ReportNode> {

	@Override
	protected void createReportSkeleton_(ReportNode parentNode,
			Select testArtefact) {
		delegate(parentNode, testArtefact, false);
	}

	@Override
	protected void execute_(ReportNode node, Select testArtefact) {
		delegate(node, testArtefact, true);
	}
	
	private void delegate(ReportNode node, Select testArtefact, boolean execution) {
		String var = testArtefact.getVar();
		
		node.setStatus(ReportNodeStatus.PASSED);
		
		for(AbstractArtefact child:getChildren(testArtefact)) {
			if(child instanceof Case) {
				Case c = (Case) ArtefactAttributesHandler.evaluateAttributes((Case) child);
				
				if(var.equals(c.getValue())) {
					if(execution) {
						ReportNode result = delegateExecute(c, node);
						node.setStatus(result.getStatus());
					} else {
						delegateCreateReportSkeleton(c, node);
					}
					break;
				} 
			}
		}
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode,
			Select testArtefact) {
		return new ReportNode();
	}

}
