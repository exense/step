package step.core.plugins;

import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;

public abstract class AbstractPlugin implements PluginCallbacks {

	@Override
	public void executionControllerStart(GlobalContext context)  throws Exception {}

	@Override
	public void executionControllerDestroy(GlobalContext context) {}

	@Override
	public void executionStart(ExecutionContext context) {}

	@Override
	public void beforeExecutionEnd(ExecutionContext context) {}

	@Override
	public void afterExecutionEnd(ExecutionContext context) {}

	@Override
	public void afterReportNodeSkeletonCreation(ReportNode node) {
	}
	
	@Override
	public void afterReportNodeExecution(ReportNode node) {
	}

	@Override
	public void associateThread(ExecutionContext context, Thread thread) {}

	@Override
	public void unassociateThread(ExecutionContext context, Thread thread) {}

}
