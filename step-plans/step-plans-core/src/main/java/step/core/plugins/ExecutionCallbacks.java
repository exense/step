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
package step.core.plugins;

import jakarta.json.JsonObject;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.functions.Function;
import step.functions.io.Output;

public interface ExecutionCallbacks {
	
	/**
	 * This hook is called before a {@link Function} is executed in CallFunctionHandler 
	 * just after the {@link Function} has been resolved
	 * The hook is also called in simulation mode
	 * 
	 * @param context the {@link ExecutionContext}
	 * @param function the resolved {@link Function} that will be executed
	 */
	void beforeFunctionExecution(ExecutionContext context, ReportNode node, Function function);
	
	/**
	 * This hook is called immediately after a {@link Function} is executed in
	 * CallFunctionHandler. The {@link ReportNode} provided as argument is therefore
	 * not reflecting the final status. If you need the final status after
	 * CallFunction execution use the hook
	 * {@link ExecutionCallbacks#afterReportNodeExecution(ExecutionContext, ReportNode)}
	 * instead. The hook is also called in simulation mode.
	 * 
	 * @param context  the {@link ExecutionContext}
	 * @param node
	 * @param function the {@link Function} that has been executed
	 * @param output   the result {@link Output} of the execution
	 */
	void afterFunctionExecution(ExecutionContext context, ReportNode node, Function function, Output<JsonObject> output);
	
	void afterReportNodeSkeletonCreation(ExecutionContext context, ReportNode node);
	
	void beforeReportNodeExecution(ExecutionContext context, ReportNode node);
	
	void afterReportNodeExecution(ExecutionContext context, ReportNode node);
	
	void onReportNodeRemoval(ExecutionContext context, ReportNode node);

	void onErrorContributionRemoval(ExecutionContext context, ReportNode node);
	
	void associateThread(ExecutionContext context, Thread thread);
	
	void associateThread(ExecutionContext context, Thread thread, long parentThreadId);
	
	void unassociateThread(ExecutionContext context, Thread thread);
	
	void beforePlanImport(ExecutionContext context);
	
	void executionStart(ExecutionContext context);

	/**
	 * This hook is called after the report skeleton creation phase.
	 * It aims to provision all resources required for the execution.
	 *
	 * @param context
	 */
	void provisionRequiredResources(ExecutionContext context);

	void deprovisionRequiredResources(ExecutionContext context);

	/**
	 * This method is only called by the ExecutionEngineRunner abort method, currently only exposed via REST service
	 * It is not part of the normal execution lifecycle
	 * @param context
	 */
	void abortExecution(ExecutionContext context);

	void forceStopExecution(ExecutionContext context);

	void afterExecutionEnd(ExecutionContext context);

	/**
	 *  This method is called after afterExecutionEnd. It is meant to be used for code that should
	 * always be executed, i.e. even if some PluginCriticalException occurred. This method should never throw
	 * a PluginCriticalException to make sure all plugins executionFinally are invoked
	 */
	void executionFinally(ExecutionContext context);
}
