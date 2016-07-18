package step.core.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import step.core.execution.ExecutionRunnable;

public class ExecutionJob implements Job {
	
	private final ExecutionRunnable runnable;
	
	public ExecutionJob(ExecutionRunnable runnable) {
		super();
		this.runnable = runnable;
	}
	
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		runnable.run();
	}

	public ExecutionRunnable getRunnable() {
		return runnable;
	}
}
