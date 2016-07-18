package step.artefacts.handlers;

import java.util.Map;
import java.util.Map.Entry;

import step.artefacts.Check;
import step.artefacts.handlers.teststep.context.BusinessError;
import step.artefacts.handlers.teststep.context.TechnicalError;
import step.artefacts.handlers.teststep.context.TestStepExecutionContext;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;

public class CheckHandler extends ArtefactHandler<Check, ReportNode> {

	public static final String STEP_CONTEXT_PARAM_KEY = "##stepcontext##";
	
	@Override
	protected void createReportSkeleton_(ReportNode parentNode, Check testArtefact) {

	}

	@Override
	protected void execute_(ReportNode node, Check testArtefact) {
		Object o = context.getVariablesManager().getVariable(STEP_CONTEXT_PARAM_KEY);
		if(o!=null && o instanceof TestStepExecutionContext) {
			TestStepExecutionContext stepContext = (TestStepExecutionContext) o;
			if(!stepContext.hasError() && !context.isSimulation()) {	
				executeChecks(stepContext, testArtefact.getSets());
			}
		}
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Check testArtefact) {
		return new ReportNode();
	}
	
	public static void executeChecks(TestStepExecutionContext stepExecutionContext, Map<String, String> checkExpressions) {
		for(Entry<String, String> entry:checkExpressions.entrySet()) {
			if(entry.getValue()!=null) {
				try {
					boolean result = stepExecutionContext.getExpressionHandler().handleCheck(entry.getValue());
					if(!result) {
						stepExecutionContext.addMessage(new BusinessError(entry.getKey()+ " (" + entry.getValue() +") returned false"));
					}
				} catch (Exception e) {
					stepExecutionContext.addMessage(new TechnicalError("Error while evaluating " + entry.getKey()+":"+e.getMessage(), e));
				}
			}
		}
	}
}
