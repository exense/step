package step.engine.plugins;

import javax.json.JsonObject;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.AbstractPlugin;
import step.functions.Function;
import step.functions.io.Output;

public abstract class AbstractExecutionEnginePlugin extends AbstractPlugin implements ExecutionEnginePlugin {

	public AbstractExecutionEnginePlugin() {
		super();
	}
	
	@Override
	public void beforePlanImport(ExecutionContext context) {}

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
	
	@Deprecated()
	public void rollbackReportNode(ReportNode node) {
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
	public void rollbackReportNode(ExecutionContext context, ReportNode node) {
		rollbackReportNode(node);
	}

	@Override
	public void associateThread(ExecutionContext context, Thread thread) {}
	
	@Override
	public void associateThread(ExecutionContext context, Thread thread, long parentThreadId) {}
	
	@Override
	public void unassociateThread(ExecutionContext context, Thread thread) {}

	@Override
	public void beforeFunctionExecution(ExecutionContext context, ReportNode node, Function function) {}

	@Override
	public void afterFunctionExecution(ExecutionContext context, ReportNode node, Function function, Output<JsonObject> output) {}

	public abstract void initialize(ExecutionEngineContext context);
}