package step.core.plans.runner;

import java.util.HashMap;
import java.util.Map;

import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;

/**
 * A simple runner that runs plans locally and doesn't support functions
 * 
 * @deprecated Use {@link ExecutionEngine} instead
 * @author Jérôme Comte
 *
 */
public class DefaultPlanRunner implements PlanRunner {

	private final ExecutionEngine engine = ExecutionEngine.builder().withPluginsFromClasspath().build();
	protected Map<String, String> properties;
	
	public DefaultPlanRunner() {
		this(null);
	}
	
	public DefaultPlanRunner(Map<String, String> properties) {
		super();
		this.properties = properties;
	}

	@Override
	public PlanRunnerResult run(Plan plan) {
		return run(plan, null);
	}

	@Override
	public PlanRunnerResult run(Plan plan, Map<String, String> executionParameters) {
		Map<String, String> mergedExecutionParameters = new HashMap<>();
		if(properties != null) {
			mergedExecutionParameters.putAll(properties);
		}
		if(executionParameters != null) {
			mergedExecutionParameters.putAll(executionParameters);
		}
		return engine.execute(plan, mergedExecutionParameters);
	}
}
