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
package step.core.execution.threadmanager;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;

@Plugin
@IgnoreDuringAutoDiscovery
public class ThreadManagerPlugin extends AbstractExecutionEnginePlugin {

	private final ThreadManager threadManager;

	public ThreadManagerPlugin(ThreadManager threadManager) {
		super();
		this.threadManager = threadManager;
	}

	@Override
	public void associateThread(ExecutionContext context, Thread thread, long parentThreadId) {
		threadManager.associateThread(context, thread, parentThreadId);
	}

	@Override
	public void associateThread(ExecutionContext context, Thread thread) {
		threadManager.associateThread(context, thread);
	}

	@Override
	public void beforeReportNodeExecution(ExecutionContext context, ReportNode node) {
		threadManager.beforeReportNodeExecution(context, node);
	}

	@Override
	public void afterReportNodeExecution(ExecutionContext context, ReportNode node) {
		threadManager.afterReportNodeExecution(context, node);
	}

	@Override
	public void unassociateThread(ExecutionContext context, Thread thread) {
		threadManager.unassociateThread(context, thread);
	}

	@Override
	public void beforeExecutionEnd(ExecutionContext context) {
		threadManager.beforeExecutionEnd(context);
	}
}
