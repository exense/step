package step.core.plugins;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;

public class AbstractExecutionPlugin extends AbstractPlugin implements ExecutionCallbacks {

	public AbstractExecutionPlugin() {
		super();
	}
	
	@Override
	public void onLocalContextCreation(ExecutionContext context) {}

	@Override
	public void executionStart(ExecutionContext context) {}

	@Override
	public void beforeExecutionEnd(ExecutionContext context) {}

	@Override
	public void afterExecutionEnd(ExecutionContext context) {}

	@Deprecated()
	public void afterReportNodeSkeletonCreation(ReportNode node) {
	}

	@Deprecated()
	public void beforeReportNodeExecution(ReportNode node) {
	}

	@Deprecated()
	public void afterReportNodeExecution(ReportNode node) {
	}

	@Override
	public void afterReportNodeSkeletonCreation(ExecutionContext context, ReportNode node) {
		afterReportNodeSkeletonCreation(node);
	}

	@Override
	public void beforeReportNodeExecution(ExecutionContext context, ReportNode node) {
		beforeReportNodeExecution(node);
	}

	@Override
	public void afterReportNodeExecution(ExecutionContext context, ReportNode node) {
		afterReportNodeExecution(node);
	}

	@Override
	public void associateThread(ExecutionContext context, Thread thread) {}

	@Override
	public void unassociateThread(ExecutionContext context, Thread thread) {}

}