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
package step.client.executions;

import java.util.concurrent.TimeoutException;

import step.client.reports.RemoteReportTreeAccessor;
import step.core.execution.model.Execution;
import step.core.plans.runner.PlanRunnerResult;

/**
 * This class represents a future of a controller execution
 *
 */
public class RemoteExecutionFuture extends PlanRunnerResult {

	private RemoteExecutionManager executionManager;
	
	public RemoteExecutionFuture(RemoteExecutionManager executionManager, String executionId) {
		super(executionId, executionId, new RemoteReportTreeAccessor(executionManager.getControllerCredentials()));
		this.executionManager = executionManager;
	}

	@Override
	public RemoteExecutionFuture waitForExecutionToTerminate(long timeout)
			throws TimeoutException, InterruptedException {
		executionManager.waitForTermination(this.executionId, timeout);
		return this;
	}
	
	public Execution getExecution() {
		return executionManager.get(this.executionId);
	}
}
