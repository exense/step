package step.core.plans.runner;

import java.util.Map;

import step.core.plans.Plan;

public interface PlanRunner {

	public PlanRunnerResult run(Plan plan);
	
	public PlanRunnerResult run(Plan plan, Map<String, String> executionParameters);
}
