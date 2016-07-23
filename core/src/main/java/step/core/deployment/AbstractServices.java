package step.core.deployment;

import javax.inject.Inject;

import step.core.Controller;
import step.core.GlobalContext;
import step.core.execution.ExecutionRunnable;
import step.core.scheduler.ExecutionScheduler;

public class AbstractServices {

	@Inject
	protected Controller controller;

	public AbstractServices() {
		super();
	}

	protected GlobalContext getContext() {
		return controller.getContext();
	}

	 ExecutionScheduler getScheduler() {
		return controller.getScheduler();
	}
	
	protected ExecutionRunnable getExecutionRunnable(String executionID) {
		for(ExecutionRunnable runnable:getScheduler().getCurrentExecutions()) {
			if(runnable.getContext().getExecutionId().equals(executionID)) {
				return runnable;
			}
		}
		return null;
	}
}