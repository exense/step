package step.artefacts.handlers.scheduler;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.TestArtefactResultHandler;
import step.core.variables.UndefinedVariableException;

public class SequentialArtefactScheduler {
	
	public void createReportSkeleton_(ReportNode node, AbstractArtefact testArtefact) {		
		for(AbstractArtefact child:ArtefactHandler.getChildren(testArtefact)) {
			ArtefactHandler.delegateCreateReportSkeleton(child, node);
		}
	}
	
	public void execute_(ReportNode node, AbstractArtefact testArtefact) {
		ExecutionContext context = ExecutionContext.getCurrentContext();
		try {
			boolean failed = false;
			boolean notCompleted = false;
			for(AbstractArtefact child:ArtefactHandler.getChildren(testArtefact)) {
				if(context.isInterrupted()) {
					node.setStatus(ReportNodeStatus.INTERRUPTED);
					break;
				}
				ReportNode resultNode = ArtefactHandler.delegateExecute(child, node);
				
				Boolean continueOnce = null;
				try {
					continueOnce = context.getVariablesManager().getVariableAsBoolean(ArtefactHandler.CONTINUE_EXECUTION_ONCE);
				} catch (UndefinedVariableException e) {} finally {
					context.getVariablesManager().removeVariable(node, ArtefactHandler.CONTINUE_EXECUTION_ONCE);												
				}
				
				if(resultNode.getStatus()==ReportNodeStatus.NOT_COMPLETED) {
					notCompleted = true;
				}
				
				if(resultNode.getStatus()==ReportNodeStatus.TECHNICAL_ERROR || resultNode.getStatus()==ReportNodeStatus.FAILED) {
					failed = true;
					if(!context.isSimulation()) {
						if(continueOnce!=null) {
							if(!continueOnce) {
								break;
							}
						} else {
							if (!context.getVariablesManager().getVariableAsBoolean(ArtefactHandler.CONTINUE_EXECUTION)) {
								break;
							}
						}
					}
				}
			}
	
			Object forcedStatus = context.getVariablesManager().getVariable("tec.forceparentstatus");
			if(forcedStatus!=null) {
				node.setStatus(ReportNodeStatus.valueOf(forcedStatus.toString()));
			} else {
				if(failed) {
					node.setStatus(ReportNodeStatus.FAILED);
				} else {
					if(!notCompleted) {
						node.setStatus(ReportNodeStatus.PASSED);
					} else {
						node.setStatus(ReportNodeStatus.NOT_COMPLETED);
					}
				}
			}
		} catch (Exception e) {
			TestArtefactResultHandler.failWithException(node, e);
		}
	}

}
