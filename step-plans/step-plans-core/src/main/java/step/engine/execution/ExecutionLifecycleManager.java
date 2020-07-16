/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.engine.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ExecutionStatus;
import step.core.plugins.ExecutionCallbacks;
import step.core.repositories.ImportResult;

public class ExecutionLifecycleManager {
	
	private final ExecutionContext context;
	
	private final ExecutionManager executionManager;
	
	private final ExecutionCallbacks executionCallbacks;
	
	private static final Logger logger = LoggerFactory.getLogger(ExecutionLifecycleManager.class);
	
	public ExecutionLifecycleManager(ExecutionContext context) {
		super();
		this.context = context;
		this.executionManager = context.getExecutionManager();
		this.executionCallbacks = context.getExecutionCallbacks();
	}

	public void abort() {
		if(context.getStatus()!=ExecutionStatus.ENDED) {
			executionManager.updateStatus(context, ExecutionStatus.ABORTING);
		}
		executionCallbacks.beforeExecutionEnd(context);
	}
	
	public void beforePlanImport() {
		executionCallbacks.beforePlanImport(context);
	}
	
	public void afterImport(ImportResult importResult) {
		executionManager.persistImportResult(context, importResult);
	}
	
	public void executionStarted() {
		executionCallbacks.executionStart(context);
	}
	
	public void executionEnded() {
		executionCallbacks.afterExecutionEnd(context);
	}
	
	public void updateStatus(ExecutionStatus newStatus) {
		executionManager.updateStatus(context,newStatus);
	}

	public void updateExecutionResult(ExecutionContext context, ReportNodeStatus resultStatus) {
		executionManager.updateExecutionResult(context, resultStatus);
	}
	
}
