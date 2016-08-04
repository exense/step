package step.artefacts.handlers;

import java.util.Map;

import step.artefacts.Case;
import step.artefacts.Switch;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactAttributesHandler;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.expressions.ExpressionHandler;

public class SwitchHandler extends ArtefactHandler<Switch, ReportNode> {

	@Override
	protected void createReportSkeleton_(ReportNode parentNode,
			Switch testArtefact) {
		delegate(parentNode, testArtefact, false);
	}

	@Override
	protected void execute_(ReportNode node, Switch testArtefact) {
		delegate(node, testArtefact, true);
	}
	
	private void delegate(ReportNode node, Switch testArtefact, boolean execution) {
		String expression = testArtefact.getExpression();
		
		node.setStatus(ReportNodeStatus.PASSED);
		
		ExpressionHandler expressionHandler = new ExpressionHandler();
		Map<String, Object> bindings = ExecutionContext.getCurrentContext().getVariablesManager().getAllVariables();
		Object evaluationResult = expressionHandler.evaluateGroovyExpression(expression, bindings);
		
		if (evaluationResult!=null) {
			if(evaluationResult instanceof String) {
				String evaluationResultStr = (String) evaluationResult;
				
				for(AbstractArtefact child:getChildren(testArtefact)) {
					if(child instanceof Case) {
						Case c = (Case) ArtefactAttributesHandler.evaluateAttributes((Case) child);
						
						if(evaluationResultStr.equals(c.getValue())) {
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
			} else {
				fail(node, "The evaluation of the expresion '"+expression+"' returned an object of type "
						+evaluationResult.getClass().getName()+" instead of a String.");
			}
		} else {
			fail(node, "The evaluation of the expresion '"+expression+"' returned null");
		}
			
		}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode,
			Switch testArtefact) {
		return new ReportNode();
	}

}
