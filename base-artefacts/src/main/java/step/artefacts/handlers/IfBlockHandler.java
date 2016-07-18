package step.artefacts.handlers;

import java.util.HashMap;

import step.artefacts.IfBlock;
import step.artefacts.handlers.scheduler.SequentialArtefactScheduler;
import step.artefacts.reports.IfBlockReportNode;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.TestArtefactResultHandler;
import step.expressions.ExpressionHandler;
import step.expressions.placeholder.PlaceHolderHandler;

public class IfBlockHandler extends ArtefactHandler<IfBlock, IfBlockReportNode> {

	@Override
	protected void createReportSkeleton_(IfBlockReportNode parentNode, IfBlock testArtefact) {
		evaluateExpressionAndDelegate(parentNode, testArtefact, false);
	}

	@Override
	protected void execute_(IfBlockReportNode node, IfBlock testArtefact) {
		evaluateExpressionAndDelegate(node, testArtefact, true);
	}

	private void evaluateExpressionAndDelegate(IfBlockReportNode node, IfBlock testArtefact, boolean execution) {
		PlaceHolderHandler placeholderHandler = new PlaceHolderHandler(ExecutionContext.getCurrentContext(), new HashMap<String, String>());
		ExpressionHandler expressionHandler = new ExpressionHandler(placeholderHandler);
		boolean checkResult;
		try {
			checkResult = expressionHandler.handleCheck(testArtefact.getCondition());
			if(checkResult) {
				SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
				if(execution) {
					scheduler.execute_(node, testArtefact);
				} else {
					scheduler.createReportSkeleton_(node, testArtefact);
				}
			} else {
				node.setStatus(ReportNodeStatus.PASSED);	
			}
		} catch (Exception e) {
			TestArtefactResultHandler.failWithException(node, e);
		}
	}

	@Override
	public IfBlockReportNode createReportNode_(ReportNode parentNode, IfBlock testArtefact) {
		return new IfBlockReportNode();
	}

}
