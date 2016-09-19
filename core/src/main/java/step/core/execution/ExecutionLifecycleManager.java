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

public class ExecutionLifecycleManager {

	private final GlobalContext context;
	
	private static final Logger logger = LoggerFactory.getLogger(ExecutionLifecycleManager.class);
	
	public ExecutionLifecycleManager(GlobalContext context) {
		super();
		this.context = context;
	}

	public void abort(ExecutionRunnable task) {
		if(task!=null && task.getContext().getStatus()!=ExecutionStatus.ENDED) {
			ExecutionStatusManager.updateStatus(task.getContext(), ExecutionStatus.ABORTING);
		}
		context.getPluginManager().getProxy().beforeExecutionEnd(task.getContext());
	}
	
	public void executionStarted(ExecutionRunnable task) {
		context.getPluginManager().getProxy().executionStart(task.getContext());
		ExecutionStatusManager.updateParameters(task.getContext());
	}
	
	public void executionEnded(ExecutionRunnable task) {
		context.getPluginManager().getProxy().afterExecutionEnd(task.getContext());
	}
	
	public void updateStatus(ExecutionRunnable runnable, ExecutionStatus newStatus) {
		ExecutionStatusManager.updateStatus(runnable.getContext(),newStatus);
	}
	
}
