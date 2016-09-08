package step.core.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
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

import step.commons.conf.Configuration;
import step.core.GlobalContext;
import step.core.execution.ExecutionRunnable;
import step.core.execution.ExecutionRunnableFactory;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionParameters;

public class Executor {
	
	private final Logger logger = LoggerFactory.getLogger(Executor.class);
			
	private Scheduler scheduler;

	private SchedulerFactory schedulerFactory;
	
	private ExecutionRunnableFactory executionRunnableFactory;
	
	public Executor(GlobalContext globalContext) {
		super();
		
		try {
			Properties props = getProperties();
			executionRunnableFactory = new ExecutionRunnableFactory(globalContext);
			schedulerFactory = new StdSchedulerFactory(props);
			scheduler = schedulerFactory.getScheduler();
			scheduler.setJobFactory(new ExecutionJobFactory(globalContext, executionRunnableFactory));
		} catch (SchedulerException e) {
			throw new RuntimeException(e);
		}
	}

	private Properties getProperties() {
		Properties props = new Properties();
		props.put("org.quartz.threadPool.threadCount", Configuration.getInstance().getProperty("tec.executor.threads", "10"));
		return props;
	}

	public void shutdown() {
		try {
			scheduler.shutdown();
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
		JobKey key = new JobKey(task.getId());
		try {
			scheduler.deleteJob(key);
		} catch (SchedulerException e) {
			logger.error("An error occurred while removing task from scheduler: " + task);
			throw new RuntimeException(e);
		}
	}

	public boolean schedule(ExecutiontTaskParameters task) {
		JobKey key = new JobKey(task.getId());
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
		Execution execution = executionRunnableFactory.createExecution(executionParameters, null);
		
		Trigger trigger = TriggerBuilder.newTrigger().startNow().build();

		String executionID = execution.getId();
		JobDetail job = buildSingleJob(executionID);
		
		scheduleJob(trigger, job);
		return executionID;
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
		data.put(EXECUTION_TASK_ID, task.getId());
		data.put(EXECUTION_PARAMETERS, task.getExecutionsParameters());
		
		return JobBuilder.newJob().ofType(ExecutionJob.class).withIdentity(task.getId()).usingJobData(data).build();
	}
	
	public List<ExecutionRunnable> getCurrentExecutions() {
		List<JobExecutionContext> excutingJobs;
		try {
			excutingJobs = scheduler.getCurrentlyExecutingJobs();
		} catch (SchedulerException e) {
			throw new RuntimeException("Unexepected error occurred while getting executing jobs", e);
		}
		List<ExecutionRunnable> result = new ArrayList<ExecutionRunnable>(excutingJobs.size());
		for(JobExecutionContext jobContext:excutingJobs) {
			result.add(((ExecutionJob) jobContext.getJobInstance()).getRunnable());
		}
		return result;
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
					System.out.println("Found job identified by: " + jobKey);
				}
			}

		} catch (SchedulerException e) {
			logger.error("An error occurred while getting scheduled jobs", e);
		}
		return result;
	}
	
}
