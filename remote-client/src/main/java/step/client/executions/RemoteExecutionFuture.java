package step.client.executions;

import java.util.concurrent.TimeoutException;

import step.client.reports.RemoteReportTreeAccessor;
import step.core.execution.model.Execution;
import step.core.plans.runner.PlanRunnerResult;

/**
 * This class represents a future of a controller execution
 *
 */
public class RemoteExecutionFuture extends PlanRunnerResult {

	private RemoteExecutionManager executionManager;
	
	public RemoteExecutionFuture(RemoteExecutionManager executionManager, String executionId) {
		super(executionId, executionId, new RemoteReportTreeAccessor(executionManager.getControllerCredentials()));
		this.executionManager = executionManager;
	}

	@Override
	public RemoteExecutionFuture waitForExecutionToTerminate(long timeout)
			throws TimeoutException, InterruptedException {
		executionManager.waitForTermination(this.executionId, timeout);
		return this;
	}
	
	public Execution getExecution() {
		return executionManager.get(this.executionId);
	}
}
