package step.artefacts.handlers;

import step.artefacts.Set;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;

public class SetHandler extends ArtefactHandler<Set, ReportNode> {

	public static final String STEP_CONTEXT_PARAM_KEY = "##stepcontext##";
	
	@Override
	protected void createReportSkeleton_(ReportNode parentNode, Set testArtefact) {

	}

	@Override
	protected void execute_(ReportNode node, Set testArtefact) {
//		Object o = context.getVariablesManager().getVariable(STEP_CONTEXT_PARAM_KEY);
//		if(o!=null && o instanceof TestStepExecutionContext) {
//			TestStepExecutionContext stepContext = (TestStepExecutionContext) o;
//			executeSets(stepContext, testArtefact.getSets());
//		}
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Set testArtefact) {
		return new ReportNode();
	}
	
//	public static void executeSets(TestStepExecutionContext stepExecutionContext, List<Entry> setExpressions) {
//		for(Entry entry:setExpressions) {
//			try {
//				stepExecutionContext.getExpressionHandler().handleSet(entry.getValue(), stepExecutionContext.getNode());
//			} catch (Exception e) {
//				String errorMsg = "Error while evaluating " + entry.getKey()+":"+e.getMessage();
//				stepExecutionContext.addMessage(new TechnicalError(errorMsg, e));
////				TestArtefactResultHandler.failWithException(ExecutionContext.getCurrentReportNode(), errorMsg, e, true);
//			}
//		}
//	}
}
