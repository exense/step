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
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import step.automation.packages.AutomationPackageEntity;
import step.automation.packages.AutomationPackageLocks;
import step.core.controller.ControllerSetting;
import step.core.controller.ControllerSettingAccessor;
import step.core.execution.ExecutionEngine;

import java.util.Objects;

import static step.automation.packages.AutomationPackageLocks.AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS;
import static step.automation.packages.AutomationPackageLocks.AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT;

public class ExecutionJob implements Job {

	public static final String SETTING_KEY_SCHEDULER_EXECUTION_USERNAME = "scheduler_execution_username";
	private final ExecutionEngine executionEngine;
	private final ExecutionTaskAccessor executionTaskAccessor;
	private final ControllerSettingAccessor controllerSettingAccessor;
	private final AutomationPackageLocks automationPackageLocks;
	private final String executionId;
	private final String executionTaskID;

	public ExecutionJob(ExecutionEngine executionEngine, ExecutionTaskAccessor executionTaskAccessor, ControllerSettingAccessor controllerSettingAccessor, AutomationPackageLocks lock, String executionId, String executionTaskID) {
		this.executionEngine = Objects.requireNonNull(executionEngine);
		this.executionTaskAccessor = Objects.requireNonNull(executionTaskAccessor);
		this.controllerSettingAccessor = Objects.requireNonNull(controllerSettingAccessor);
		this.automationPackageLocks = Objects.requireNonNull(lock);
		this.executionId = executionId;
		this.executionTaskID = executionTaskID;
	}

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		if (executionId != null) {
			executionEngine.execute(executionId);
		} else if (executionTaskID != null) {
			String automationPackageLockID = null;
			try {
				ExecutiontTaskParameters executiontTaskParameters = executionTaskAccessor.get(new ObjectId(executionTaskID));
				if (executiontTaskParameters == null) {
					throw new JobExecutionException("The execution task parameters for schedule ID '" + executionTaskID + "' were not found in the database. This may be due to a race condition where a schedule is being deleted at the same time as its job is triggered.");
				}
				//Try to get the read lock on automation package even before creating the execution data, otherwise data get out dated
				automationPackageLockID = (String) executiontTaskParameters.getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID);
				if (automationPackageLockID != null) {
					try {
						if (!automationPackageLocks.tryReadLock(automationPackageLockID)) {
							throw new JobExecutionException("Timeout while acquiring lock on automation package with id " +
									automationPackageLockID + ". This usually means that an update of this automation package is on-going and took more than the property " +
									AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS + " (default " + AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT + " seconds)");
						}
					} catch (InterruptedException e) {
						throw new JobExecutionException(e);
					}
					//Get the latest version from DB in case of lock
					executiontTaskParameters = executionTaskAccessor.get(new ObjectId(executionTaskID));
					// Error handling in case the schedule got deleted in between
					if (executiontTaskParameters == null) {
						throw new JobExecutionException("The execution task parameters for schedule ID '" + executionTaskID + "' were not found in the database. This may be due to a race condition where a schedule is being deleted at the same time as its job is triggered.");
					}
				}

				ControllerSetting schedulerUsernameSetting = controllerSettingAccessor.getSettingByKey(SETTING_KEY_SCHEDULER_EXECUTION_USERNAME);
				if (schedulerUsernameSetting != null) {
					String schedulerUsername = schedulerUsernameSetting.getValue();
					if (schedulerUsername != null && schedulerUsername.trim().length() > 0) {
						// Override the execution user if the setting scheduler_execution_username is set
						executiontTaskParameters.getExecutionsParameters().setUserID(schedulerUsername);
					}
				}

				String executionId = executionEngine.initializeExecution(executiontTaskParameters);
				executionEngine.execute(executionId);
			} finally {
				if (automationPackageLocks != null && automationPackageLockID != null) {
					automationPackageLocks.readUnlock(automationPackageLockID);
				}
			}
		} else {
			throw new JobExecutionException("The job is missing both execution ID and schedule ID");
		}
	}
}
