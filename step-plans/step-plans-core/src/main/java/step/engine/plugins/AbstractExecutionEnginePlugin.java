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
package step.engine.plugins;

import jakarta.json.JsonObject;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.AbstractExecutionEngineContext;
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
	public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext executionEngineContext) {}
	
	@Override
	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {}
	
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
	public void beforePlanImport(ExecutionContext context) {}

	@Override
	public void beforeFunctionExecution(ExecutionContext context, ReportNode node, Function function) {}

	@Override
	public void afterFunctionExecution(ExecutionContext context, ReportNode node, Function function, Output<JsonObject> output) {}
}
