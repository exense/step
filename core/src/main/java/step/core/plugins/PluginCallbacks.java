package step.core.plugins;

import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;

public interface PluginCallbacks {

	public void executionControllerStart(GlobalContext context) throws Exception;
	
	public void executionControllerDestroy(GlobalContext context);
	
	public void executionStart(ExecutionContext context);

	public void beforeExecutionEnd(ExecutionContext context);
	
	public void afterExecutionEnd(ExecutionContext context);
	
	public void afterReportNodeSkeletonCreation(ReportNode node);
	
	public void afterReportNodeExecution(ReportNode node);
	
	public void associateThread(ExecutionContext context, Thread thread);
	
	public void unassociateThread(ExecutionContext context, Thread thread);
}
