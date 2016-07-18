package step.core.scheduler;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import step.core.GlobalContext;
import step.core.execution.ExecutionRunnable;
import step.core.execution.ExecutionRunnableFactory;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionParameters;

public class ExecutionJobFactory implements JobFactory {

	private GlobalContext context;
	
	private ExecutionRunnableFactory executionRunnableFactory;

	public ExecutionJobFactory(GlobalContext context, ExecutionRunnableFactory executionRunnableFactory) {
		super();
		this.context = context;
		this.executionRunnableFactory = executionRunnableFactory;
	}

	@Override
	public Job newJob(TriggerFiredBundle arg0, Scheduler arg1) throws SchedulerException {
		JobDataMap data = arg0.getJobDetail().getJobDataMap();
		ExecutionRunnable task;
		Execution execution;
		if(data.containsKey(Executor.EXECUTION_ID)) {
			String executionID = data.getString(Executor.EXECUTION_ID);
			execution = context.getExecutionAccessor().get(executionID);
		} else {
			String executionTaskID = data.getString(Executor.EXECUTION_TASK_ID);
			ExecutionParameters executionParams = (ExecutionParameters) data.get(Executor.EXECUTION_PARAMETERS);
			execution = executionRunnableFactory.createExecution(executionParams, executionTaskID);			
		}
		task = executionRunnableFactory.newExecutionRunnable(execution);
		
		ExecutionJob job = new ExecutionJob(task);
		return job;
	}
}
