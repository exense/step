package step.junit.runner;

import step.core.plans.Plan;

public class StepClassParserResult {

	private final String name;
	private final Plan plan;
	private final Exception initializingException;

	public StepClassParserResult(String name, Plan plan, Exception initializingException) {
		super();
		this.name = name;
		this.plan = plan;
		this.initializingException = initializingException;
	}

	public String getName() {
		return name;
	}

	public Plan getPlan() {
		return plan;
	}

	public Exception getInitializingException() {
		return initializingException;
	}
}