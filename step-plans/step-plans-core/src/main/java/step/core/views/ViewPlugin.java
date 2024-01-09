/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.views;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;

@Plugin
public class ViewPlugin extends AbstractExecutionEnginePlugin {
	
	private ViewManager viewManager;

	@Override
	public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
		viewManager = context.inheritFromParentOrComputeIfAbsent(parentContext, ViewManager.class, k->new ViewManager(new InMemoryViewModelAccessor()));
	}

	//We need the view manager in the execution context for the after execution hook even if the plan import fail
	@Override
	public void beforePlanImport(ExecutionContext context) {
		context.put(ViewManager.class, viewManager);
		viewManager.createViewModelsForExecution(context.getExecutionId());
	}

	//Plan import might not be called (i.e. interactive executions)
	@Override
	public void executionStart(ExecutionContext context) {
		context.put(ViewManager.class, viewManager);
		viewManager.createViewModelsForExecution(context.getExecutionId());
	}

	@Override
	public void afterExecutionEnd(ExecutionContext context) {
		viewManager.closeViewModelsForExecution(context.getExecutionId());
	}

	@Override
	public void beforeReportNodeExecution(ExecutionContext context, ReportNode node){
		viewManager.beforeReportNodeExecution(node);
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
	public void onReportNodeRemoval(ExecutionContext context, ReportNode node) {
		viewManager.onReportNodeRemoval(node);
	}

	@Override
	public void onErrorContributionRemoval(ExecutionContext context, ReportNode node) {
		viewManager.onErrorContributionRemoval(node);
	}
}
