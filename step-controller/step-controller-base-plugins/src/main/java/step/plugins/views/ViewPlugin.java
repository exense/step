package step.plugins.views;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.engine.plugins.AbstractExecutionEnginePlugin;

public class ViewPlugin extends AbstractExecutionEnginePlugin {
	
	private final ViewManager viewManager;

	public ViewPlugin(ViewManager viewManager) {
		super();
		this.viewManager = viewManager;
	}

	@Override
	public void executionStart(ExecutionContext context) {
		viewManager.createViewModelsForExecution(context.getExecutionId());
	}

	@Override
	public void afterExecutionEnd(ExecutionContext context) {
		viewManager.closeViewModelsForExecution(context.getExecutionId());
	}

	@Override
	public void afterReportNodeSkeletonCreation(ReportNode node) {
		viewManager.afterReportNodeSkeletonCreation(node);
	}

	@Override
	public void afterReportNodeExecution(ReportNode node) {
		viewManager.afterReportNodeExecution(node);
	}
	
	@Override
	public void rollbackReportNode(ReportNode node) {
		viewManager.rollbackReportNode(node);
	}
}
