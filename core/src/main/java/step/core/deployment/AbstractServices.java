package step.core.deployment;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.ResourceConfig;

import step.core.Controller;
import step.core.GlobalContext;
import step.core.execution.ExecutionRunnable;
import step.core.scheduler.ExecutionScheduler;

public class AbstractServices {

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
	
	@Context
	public void setController(Application controller) {
		// this is ugly. Reason is that jersey injects a wrapper of the application instead of the instance
		this.controller = ((ControllerApplication) ((ResourceConfig) controller).getApplication()).getController();
	}

}