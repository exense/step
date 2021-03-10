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
	public void rollbackReportNode(ReportNode node) {
		viewManager.rollbackReportNode(node);
	}
}
