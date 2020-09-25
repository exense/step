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
package step.core.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import step.core.execution.ExecutionEngine;

public class ExecutionJob implements Job {
	
	private final ExecutionEngine executionEngine;
	private final String executionId;
	
	public ExecutionJob(ExecutionEngine executionEngine, String executionId) {
		super();
		this.executionEngine = executionEngine;
		this.executionId = executionId;
	}

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		executionEngine.execute(executionId);
	}
}
