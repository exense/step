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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.AbstractOrganizableObject;
import step.core.controller.ControllerSettingAccessor;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ExecutionLauncher;
import step.core.execution.model.ExecutionParameters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExecutionScheduler implements ExecutionLauncher {
	
	private final Logger logger = LoggerFactory.getLogger(ExecutionScheduler.class);
	
	private final ControllerSettingAccessor controllerSettingAccessor;
	private final ExecutionTaskAccessor executionTaskAccessor;		
	private final Executor executor;
	private List<ExecutionSchedulerHook> executionSchedulerHooks = new ArrayList<>();
	
	public static final String SETTING_SCHEDULER_ENABLED = "scheduler_enabled";
		
	public ExecutionScheduler(ControllerSettingAccessor controllerSettingAccessor,
			ExecutionTaskAccessor executionTaskAccessor, Executor executor) {
		super();
		this.controllerSettingAccessor = controllerSettingAccessor;
		this.executionTaskAccessor = executionTaskAccessor;
		this.executor = executor;
	}

	public Executor getExecutor() {
		return executor;
	}

	public void shutdown() {
		executor.shutdown();
	}

	public void start() {
		executor.start();
		loadExecutionTasks();
	}
	
	private void loadExecutionTasks() {
		if(isSchedulerEnabled()) {
			Iterator<ExecutiontTaskParameters> it = getActiveExecutionTasks();
			while(it.hasNext()) {
				ExecutiontTaskParameters task = it.next();
				logger.info("Loading schedule for task '" + task.getAttribute(AbstractOrganizableObject.NAME) + "' having for id : " + task.getId());
				try {
					boolean mayFireAgain = executor.schedule(task);
					if(!mayFireAgain) {
						removeExecutionTask(task.getId().toString());
					}
				} catch (Exception e) {
					logger.error("An error occurred while scheduling task. "+ task.toString()+ ". Disabling task.", e);
					disableExecutionTask(task.getId().toString());
				}
			}
		}		
	}
	
	public Iterator<ExecutiontTaskParameters> getActiveExecutionTasks() {
		return executionTaskAccessor.getActiveExecutionTasks();
	}
	
	public Iterator<ExecutiontTaskParameters> getActiveAndInactiveExecutionTasks() {
		return executionTaskAccessor.getAll();
	}
	
	public void removeExecutionTask(String executionTaskID) {
		ExecutiontTaskParameters task = get(executionTaskID);
		remove(task);
		executor.deleteSchedule(task);
		runOnRemoveExecutionTasks(executionTaskID);
	}
	
	public void enableExecutionTask(String executionTaskID) {
		ExecutiontTaskParameters task = get(executionTaskID);
		task.setActive(true);
		save(task);
		if(isSchedulerEnabled()) {
			executor.schedule(task);			
		} 
	}
	
	public void disableExecutionTask(String executionTaskID) {
		ExecutiontTaskParameters task = get(executionTaskID);
		task.setActive(false);
		save(task);
		executor.deleteSchedule(task);
	}

	public boolean addOrUpdateExecutionTask(ExecutiontTaskParameters task) {
		return addOrUpdateExecutionTask(task, true);
	}

	public boolean addOrUpdateExecutionTask(ExecutiontTaskParameters task, boolean autoActivateNewTask) {
		executor.validate(task);
		if (autoActivateNewTask && !taskAlreadyExists(task)) {
			task.setActive(true);
		}
		boolean triggerMayFireAgain = true;
		//Activate the tasks if scheduler is enabled and active flag is ON
		if (isSchedulerEnabled() && task.isActive()) {
			triggerMayFireAgain = executor.schedule(task);
		} else if (taskAlreadyExists(task) && !task.isActive()) {
			//In case of update with active flag OFF, make sure to remove the related schedule if one exists
			executor.deleteScheduleIfRequired(task);
			triggerMayFireAgain = false;
		}
		//Only save task if it could be scheduled
		save(task);
		return triggerMayFireAgain;
	}
	
	private boolean taskAlreadyExists(ExecutiontTaskParameters task) {
		boolean exist = false;
		Iterator<ExecutiontTaskParameters> it = getActiveAndInactiveExecutionTasks();
		while(it.hasNext()) {
			ExecutiontTaskParameters taskToCompare = it.next();
			exist = task.equals(taskToCompare);
			if(exist) break;
		}
		return exist;
	}

	private boolean isSchedulerEnabled() {
		return controllerSettingAccessor.getSettingAsBoolean(SETTING_SCHEDULER_ENABLED);
	}

	@Override
	public String execute(ExecutionParameters executionParameters) {
		return executor.execute(executionParameters);
	}

	public String executeNested(ExecutionParameters executionParameters) {
		return executor.executeNested(executionParameters);
	}

	public String executeExecutionTask(String executionTaskID, String user) {
		ExecutiontTaskParameters task = get(executionTaskID);
		task.getExecutionsParameters().setUserID(user);
		return executor.execute(task);
	}
	
	public ExecutiontTaskParameters get(String id) {
		return executionTaskAccessor.get(new ObjectId(id));
	}
	
	private void save(ExecutiontTaskParameters schedule) {
		executionTaskAccessor.save(schedule);
	}
	
	private void remove(ExecutiontTaskParameters schedule) {
		executionTaskAccessor.remove(schedule.getId());
	}
	
	public List<ExecutionContext> getCurrentExecutions() {
		return executor.getCurrentExecutions();
	}

	public void enableAllExecutionTasksSchedule() {
		// Save setting
		controllerSettingAccessor.updateOrCreateSetting(SETTING_SCHEDULER_ENABLED, Boolean.TRUE.toString());
		
		logger.info("Enabling Scheduler...");
		loadExecutionTasks();	
	}
	
	public void disableAllExecutionTasksSchedule() {
		// Save setting
		controllerSettingAccessor.updateOrCreateSetting(SETTING_SCHEDULER_ENABLED, Boolean.FALSE.toString());
		
		logger.info("Disabling Scheduler...");
		Iterator<ExecutiontTaskParameters> it = getActiveExecutionTasks();
		while(it.hasNext()) {
			ExecutiontTaskParameters task = it.next();
			logger.info("Disabling schedule for task '" + task.getAttribute(AbstractOrganizableObject.NAME) + "' having for id : " + task.getId());
			executor.deleteSchedule(task);
		}
	}
	
	public void registerExecutionSchedulerHook(ExecutionSchedulerHook executionSchedulerHook) {
		executionSchedulerHooks.add(executionSchedulerHook);
	}
	
	private void runOnRemoveExecutionTasks(String executionTaskId){
		executionSchedulerHooks.forEach(e -> e.onRemoveExecutionTask(executionTaskId));
	}

	public ExecutionTaskAccessor getExecutionTaskAccessor() {
		return executionTaskAccessor;
	}
}
