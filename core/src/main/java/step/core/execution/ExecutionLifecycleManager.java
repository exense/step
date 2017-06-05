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
package step.core.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.execution.model.ExecutionStatus;
import step.core.plugins.PluginManager;
import step.core.repositories.Repository.ImportResult;

public class ExecutionLifecycleManager {
	
	private final ExecutionContext context;
	
	private final ExecutionManager executionManager;
	
	private final PluginManager pluginManager;
	
	private static final Logger logger = LoggerFactory.getLogger(ExecutionLifecycleManager.class);
	
	public ExecutionLifecycleManager(ExecutionContext context) {
		super();
		this.context = context;
		
		GlobalContext globalContext = context.getGlobalContext();
		this.executionManager = new ExecutionManager(globalContext);
		this.pluginManager = globalContext.getPluginManager();
	}

	public void abort() {
		if(context.getStatus()!=ExecutionStatus.ENDED) {
			executionManager.updateStatus(context, ExecutionStatus.ABORTING);
		}
		pluginManager.getProxy().beforeExecutionEnd(context);
	}
	
	public void afterImport(ImportResult importResult) {
		executionManager.persistImportResult(context, importResult);
	}
	
	public void executionStarted() {
		pluginManager.getProxy().executionStart(context);
		executionManager.updateParameters(context);
	}
	
	public void executionEnded() {
		pluginManager.getProxy().afterExecutionEnd(context);
	}
	
	public void updateStatus(ExecutionStatus newStatus) {
		executionManager.updateStatus(context,newStatus);
	}
	
}
