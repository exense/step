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

import org.bson.types.ObjectId;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import step.automation.packages.AutomationPackageLocks;
import step.core.GlobalContext;
import step.core.controller.ControllerSettingAccessor;
import step.core.execution.ExecutionEngine;

public class ExecutionJobFactory implements JobFactory {

	private final ExecutionEngine executionEngine;
	private final ControllerSettingAccessor controllerSettingAccessor;
	private final ExecutionTaskAccessor executionTaskAccessor;
	private final AutomationPackageLocks automationPackageLocks;

	public ExecutionJobFactory(GlobalContext context, ExecutionEngine executionEngine) {
		super();
		this.controllerSettingAccessor = context.require(ControllerSettingAccessor.class);
		this.executionEngine = executionEngine;
		this.executionTaskAccessor = context.getScheduleAccessor();
		this.automationPackageLocks = context.require(AutomationPackageLocks.class);
	}

	@Override
	public Job newJob(TriggerFiredBundle arg0, Scheduler arg1) throws SchedulerException {
		JobDataMap data = arg0.getJobDetail().getJobDataMap();
		String executionID = null;
		String executionTaskID = null;
		if (data.containsKey(Executor.EXECUTION_ID)) {
			executionID = data.getString(Executor.EXECUTION_ID);
		} else {
			try {
				executionTaskID = data.getString(Executor.EXECUTION_TASK_ID);
			} catch (Exception e) {
				return new LoggerJob("Unable to create new job for execution", e);
			}
		}
		return new ExecutionJob(executionEngine, executionTaskAccessor, controllerSettingAccessor, automationPackageLocks, executionID, executionTaskID);
	}
}
