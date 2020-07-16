package step.core.plans.runner;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineException;
import step.engine.PlanRunnerImpl;

/**
 * A simple runner that runs plans locally and doesn't support functions
 * 
 * @deprecated Use {@link PlanRunnerImpl} instead
 * @author Jérôme Comte
 *
 */
public class DefaultPlanRunner extends PlanRunnerImpl implements PlanRunner {

	public DefaultPlanRunner() {
		super(newContext());
	}
	
	public DefaultPlanRunner(ExecutionContext executionContext) {
		super(executionContext);
	}
	
	private static ExecutionContext newContext() {
		try {
			return new ExecutionEngine().newExecutionContext();
		} catch (ExecutionEngineException e) {
			// rethrow as runtime exception in order to keep the signature of the constructor unchanged
			throw new RuntimeException(e);
		}
	}
}
