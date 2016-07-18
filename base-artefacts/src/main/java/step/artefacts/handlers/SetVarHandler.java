package step.artefacts.handlers;

import java.util.HashMap;

import step.artefacts.Entry;
import step.artefacts.SetVar;
import step.artefacts.reports.SetVarReportNode;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.TestArtefactResultHandler;
import step.expressions.ExpressionHandler;
import step.expressions.placeholder.PlaceHolderHandler;

public class SetVarHandler extends ArtefactHandler<SetVar, SetVarReportNode> {

	@Override
	protected void createReportSkeleton_(SetVarReportNode node, SetVar testArtefact) {
		handleSets(node, testArtefact);
	}

	@Override
	protected void execute_(SetVarReportNode node, SetVar testArtefact) {
		handleSets(node, testArtefact);
	}
	
	protected void handleSets(ReportNode node, SetVar testArtefact) {
		PlaceHolderHandler placeholderHandler = new PlaceHolderHandler(ExecutionContext.getCurrentContext(), new HashMap<String, String>());
		ExpressionHandler expressionHandler = new ExpressionHandler(placeholderHandler);
		
		node.setStatus(ReportNodeStatus.PASSED);
		
		for(Entry entry:testArtefact.getSets()) {
			try {
				expressionHandler.handleSet(entry.getValue(), node);
			} catch (Exception e) {
				TestArtefactResultHandler.failWithException(node, "Error while evaluating " + entry.getKey(),e, true);
			}
		}
	}

	@Override
	public SetVarReportNode createReportNode_(ReportNode parentNode, SetVar testArtefact) {
		return new SetVarReportNode();
	}
}
