package step.core.plans.runner;

import java.util.HashMap;
import java.util.Map;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.Plan;

/**
 * A simple runner that runs plans locally and doesn't support functions
 * 
 * @deprecated Use {@link ExecutionEngine} instead
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
			ExecutionParameters executionParametersObject = executionContext.getExecutionParameters();
			Map<String, String> customParameters = buildExecutionParametersIfNull(executionParametersObject.getCustomParameters());
			executionParametersObject.setCustomParameters(customParameters);
			executionParametersObject.setPlan(plan);
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
