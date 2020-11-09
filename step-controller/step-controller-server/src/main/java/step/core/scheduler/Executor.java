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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.OperationMode;
import step.core.execution.model.ExecutionParameters;
import step.core.objectenricher.ObjectHookRegistry;
import step.engine.plugins.ExecutionEnginePlugin;

public class Executor {
	
	private final Logger logger = LoggerFactory.getLogger(Executor.class);
			
	private Scheduler scheduler;
	private SchedulerFactory schedulerFactory;
	private ExecutionEngine executionEngine;
	private Configuration configuration;
	
	public Executor(GlobalContext globalContext) {
		super();
		
		configuration = globalContext.getConfiguration();
		
		List<ExecutionEnginePlugin> additionalPlugins = globalContext.getPluginManager().getExecutionEnginePlugins();
		
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

	public Executor() {
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
		} catch (SchedulerException e) {
			logger.error("An error occurred while removing task from scheduler: " + task);
			throw new RuntimeException(e);
		}
	}
	
	public void validate(ExecutiontTaskParameters task) {
		CronScheduleBuilder.cronSchedule(task.getCronExpression());
	}

	public boolean schedule(ExecutiontTaskParameters task) {
		JobKey key = new JobKey(task.getId().toString());
		try {
			if(scheduler.checkExists(key)) {
				deleteSchedule(task);
			}
		} catch (SchedulerException e) {
			logger.error("An error occurred while checking if task exists in scheduler: " + task);
			throw new RuntimeException(e);
		}
		Trigger trigger = TriggerBuilder.newTrigger().withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression())).build();
		JobDetail job = buildScheduledJob(task);
		scheduleJob(trigger, job);
		return trigger.mayFireAgain();
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
