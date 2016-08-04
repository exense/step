package step.artefacts.handlers;

import java.util.Map;

import step.artefacts.Set;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.variables.UndefinedVariableException;
import step.core.variables.VariablesManager;
import step.expressions.ExpressionHandler;

public class SetHandler extends ArtefactHandler<Set, ReportNode> {
	
	@Override
	protected void createReportSkeleton_(ReportNode parentNode, Set testArtefact) {

	}

	@Override
	protected void execute_(ReportNode node, Set testArtefact) {
		node.setStatus(ReportNodeStatus.PASSED);
		if(testArtefact.getKey()!=null) {
			Object result;			
			if(testArtefact.getExpression()!=null) {
				ExpressionHandler expressionHandler = new ExpressionHandler();
				Map<String, Object> bindings = ExecutionContext.getCurrentContext().getVariablesManager().getAllVariables();
				result= expressionHandler.evaluateGroovyExpression(testArtefact.getExpression(), bindings);				
			} else {
				result = null;
			}				
			
			VariablesManager varMan = context.getVariablesManager();
			try {
				varMan.updateVariable(testArtefact.getKey(), result);
			} catch(UndefinedVariableException e) {
				ReportNode parentNode = context.getReportNodeCache().get(node.getParentID().toString());
				varMan.putVariable(parentNode, testArtefact.getKey(), result);
			}
		}
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Set testArtefact) {
		return new ReportNode();
	}
}
