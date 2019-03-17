package step.core.plans.runner;

import java.util.Map;

import step.core.plans.Plan;

public interface PlanRunner {

	/**
	 * Runs a plan instance
	 * 
	 * @param plan the plan to be run
	 * @return an handle to the execution result
	 */
	public PlanRunnerResult run(Plan plan);
	
	/**
	 * Runs a plan instance using the provided execution parameters
	 * 
	 * @param plan thje plan to be run
	 * @param executionParameters the execution parameters to be used for the execution. 
	 * These parameters are equivalent to the parameters selected on the execution screen of the STEP UI
	 * @return  an handle to the execution result
	 */
	public PlanRunnerResult run(Plan plan, Map<String, String> executionParameters);
}
