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

import ch.exense.commons.app.Configuration;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.calendar.BaseCalendar;
import org.quartz.impl.calendar.CronCalendar;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.OperationMode;
import step.core.execution.model.ExecutionParameters;
import step.core.objectenricher.ObjectHookRegistry;
import step.engine.plugins.ExecutionEnginePlugin;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static step.core.accessors.AbstractOrganizableObject.NAME;

public class Executor {
	
	private final Logger logger = LoggerFactory.getLogger(Executor.class);
			
	private Scheduler scheduler;
	private SchedulerFactory schedulerFactory;
	private ExecutionEngine executionEngine;
	private Configuration configuration;
	
	public Executor(GlobalContext globalContext) {
		super();
		
		configuration = globalContext.getConfiguration();
		
		List<ExecutionEnginePlugin> additionalPlugins = globalContext.getControllerPluginManager().getExecutionEnginePlugins();
		
		ObjectHookRegistry objectHookRegistry = globalContext.require(ObjectHookRegistry.class);
		
		executionEngine = ExecutionEngine.builder().withOperationMode(OperationMode.CONTROLLER)
				.withParentContext(globalContext).withPluginsFromClasspath().withPlugins(additionalPlugins).withObjectHookRegistry(objectHookRegistry).build();
		
		try {
			Properties props = getProperties();
			schedulerFactory = new StdSchedulerFactory(props);
			scheduler = schedulerFactory.getScheduler();
			scheduler.setJobFactory(new ExecutionJobFactory(globalContext, executionEngine));
		} catch (SchedulerException e) {
			throw new RuntimeException(e);
		}
	}

	protected Executor() {
	}

	protected Executor(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	private Properties getProperties() {
		Properties props = new Properties();
		props.put("org.quartz.threadPool.threadCount", configuration.getProperty("tec.executor.threads", "10"));
		return props;
	}

	public void shutdown() {
		try {
			scheduler.shutdown(true);
		} catch (SchedulerException e) {
			throw new RuntimeException(e);
		}
	}

	public void start() {
		try {
			scheduler.start();
		} catch (SchedulerException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void deleteSchedule(ExecutiontTaskParameters task) {
		JobKey key = new JobKey(task.getId().toString());
		try {
			scheduler.deleteJob(key);
			scheduler.deleteCalendar(getExclusionCalendarName(task));
		} catch (SchedulerException e) {
			logger.error("An error occurred while removing task from scheduler: " + task);
			throw new RuntimeException(e);
		}
	}
	
	public void validate(ExecutiontTaskParameters task) {
		try {
			CronScheduleBuilder.cronSchedule(task.getCronExpression());
			List<CronExclusion> cronExclusions = task.getCronExclusions();
			if (cronExclusions != null) {
				cronExclusions.forEach(c-> CronScheduleBuilder.cronSchedule(c.getCronExpression()));
			}
		} catch (RuntimeException e) {
			logAndThrow(e.getMessage(), e);
		}
	}

	public boolean schedule(ExecutiontTaskParameters task) {
		JobKey key = new JobKey(task.getId().toString());
		String taskName = task.getAttribute(NAME);
		try {
			if(scheduler.checkExists(key)) {
				deleteSchedule(task);
			}
		} catch (SchedulerException e) {
			logger.error("An error occurred while checking if task exists in scheduler: " + task);
			throw new RuntimeException(e);
		}
		//Base calendar allows you to chain calendars.
		BaseCalendar baseCalendar = new BaseCalendar();
		if (task.getCronExclusions() != null) {
			try {
				for(CronExclusion c : task.getCronExclusions()) {
					baseCalendar = new CronCalendar(baseCalendar, c.getCronExpression());
				}
			} catch (ParseException e) { //such exception should already be caught by validate
				logAndThrow("One of the cron expressions for the task '" + taskName +
						"' is invalid.", e);
			}
		}
		//In case exclusions were removed we need to add/replace at least the base calendar
		String exclusionCalendarName = getExclusionCalendarName(task);
		try {
			scheduler.addCalendar(exclusionCalendarName, baseCalendar, true, false);
		} catch (SchedulerException e) {//exception throw when adding exclustion calendar
			logAndThrow("Adding exclusion CRON expressions raised an error for the task '" +
					taskName + "'.", e);
		}

		Trigger trigger = TriggerBuilder.newTrigger()
				.withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression()))
				.modifiedByCalendar(exclusionCalendarName)
				.build();
		JobDetail job = buildScheduledJob(task);
		try {
			scheduleJob(trigger, job);
		} catch (Exception e) {
			logAndThrow("Unable to schedule task '" + taskName + "'.", e);
		}
		return trigger.mayFireAgain();
	}

	private String getExclusionCalendarName(ExecutiontTaskParameters task) {
		return "exclusionsCalendar_" + task.getId().toHexString();
	}

	private void logAndThrow(String message, Exception e) {
		String exMessage = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
		logger.error(message, e);
		throw new RuntimeException(message + " Details: " + exMessage);
	}

	public String execute(ExecutionParameters executionParameters) {
		String executionID = executionEngine.initializeExecution(executionParameters);
		scheduleExistingExecutionNow(executionID);
		return executionID;
	}
	
	public String execute(ExecutiontTaskParameters executionTaskParameters) {
		String executionID = executionEngine.initializeExecution(executionTaskParameters);
		scheduleExistingExecutionNow(executionID);
		return executionID;
	}

	private void scheduleExistingExecutionNow(String executionID) {
		Trigger trigger = TriggerBuilder.newTrigger().startNow().build();

		JobDetail job = buildSingleJob(executionID);
		
		scheduleJob(trigger, job);
	}

	private void scheduleJob(Trigger trigger, JobDetail job) {
		try {
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException e) {
			throw new RuntimeException("An unexpected error occurred while scheduling job "+ job.toString(), e);
		}
	}
	
	protected static final String EXECUTION_PARAMETERS = "ExecutionParameters";
	
	protected static final String EXECUTION_ID = "ExecutionID";

	protected static final String EXECUTION_TASK_ID = "ExecutionTaskID";
	
	private JobDetail buildSingleJob(String executionID) {		
		JobDataMap data = new JobDataMap();
		data.put(EXECUTION_ID, executionID);
		
		return JobBuilder.newJob().ofType(ExecutionJob.class).usingJobData(data).build();
	}
	
	private JobDetail buildScheduledJob(ExecutiontTaskParameters task) {
		JobDataMap data = new JobDataMap();
		data.put(EXECUTION_TASK_ID, task.getId().toString());
		data.put(EXECUTION_PARAMETERS, task.getExecutionsParameters());
		
		return JobBuilder.newJob().ofType(ExecutionJob.class).withIdentity(task.getId().toString()).usingJobData(data).build();
	}
	
	public List<ExecutionContext> getCurrentExecutions() {
		return executionEngine.getCurrentExecutions();
	}
	
	public List<ExecutiontTaskParameters> getScheduledExecutions() {
		List<ExecutiontTaskParameters> result = new ArrayList<>();
		try {
			for (String group : scheduler.getJobGroupNames()) {
				GroupMatcher<JobKey> matcher = GroupMatcher.groupEquals(group);
				for (JobKey jobKey : scheduler.getJobKeys(matcher)) {
					JobDetail job = scheduler.getJobDetail(jobKey);
					JobDataMap data = job.getJobDataMap();
					ExecutionParameters params = (ExecutionParameters) data.get(EXECUTION_PARAMETERS);
					
					List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
					for(Trigger trigger:triggers) {
						if(trigger instanceof CronTrigger) {
							ExecutiontTaskParameters p = 
									new ExecutiontTaskParameters(params, ((CronTrigger)trigger).getCronExpression());
							result.add(p);
						}
					}
				}
			}

		} catch (SchedulerException e) {
			logger.error("An error occurred while getting scheduled jobs", e);
		}
		return result;
	}
	
}
