package step.core.plans.runner;

import java.util.HashMap;
import java.util.Map;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineRunner;
import step.core.plans.Plan;

/**
 * A simple runner that runs plans locally and doesn't support functions
 * 
 * @deprecated Use {@link ExecutionEngineRunner} instead
 * @author Jérôme Comte
 *
 */
public class DefaultPlanRunner implements PlanRunner {

	private final ExecutionContext executionContext;
	private final ExecutionEngine engine = new ExecutionEngine();
	protected Map<String, String> properties;
	
	public DefaultPlanRunner() {
		this(null);
	}
	
	public DefaultPlanRunner(ExecutionContext executionContext) {
		this(executionContext, null);
	}
	
	public DefaultPlanRunner(ExecutionContext executionContext, Map<String, String> properties) {
		super();
		this.executionContext = executionContext;
		this.properties = properties;
	}

	@Override
	public PlanRunnerResult run(Plan plan) {
		return run(plan, null);
	}

	@Override
	public PlanRunnerResult run(Plan plan, Map<String, String> executionParameters) {
		if(executionContext == null) {
			executionParameters = buildExecutionParametersIfNull(executionParameters);
			return engine.execute(plan, executionParameters);
		} else {
			Map<String, String> customParameters = buildExecutionParametersIfNull(executionContext.getExecutionParameters().getCustomParameters());
			executionContext.getExecutionParameters().setCustomParameters(customParameters);
			return engine.execute(executionContext);
		}
	}

	protected Map<String, String> buildExecutionParametersIfNull(Map<String, String> executionParameters) {
		if(properties != null) {
			if(executionParameters == null) {
				executionParameters = new HashMap<>();
			}
			executionParameters.putAll(properties);
		}
		return executionParameters;
	}
}
