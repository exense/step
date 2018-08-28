package step.core.plans.runner;

import java.util.ArrayList;
import java.util.Map;

import step.core.artefacts.handlers.ArtefactHandler;
import step.core.execution.ContextBuilder;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;

/**
 * A simple runner that runs plans locally and doesn't support functions
 * 
 * @author Jérôme Comte
 *
 */
public class DefaultPlanRunner implements PlanRunner {

	@Override
	public PlanRunnerResult run(Plan plan) {
		ExecutionContext context = ContextBuilder.createLocalExecutionContext();
		context.getArtefactAccessor().save(new ArrayList<>(plan.getArtefacts()));
		ArtefactHandler.delegateExecute(context, plan.getRoot(),context.getReport());
		return new PlanRunnerResult(context.getExecutionId(), context.getReport().getId().toString(), context.getReportNodeAccessor());
	}

	@Override
	public PlanRunnerResult run(Plan plan, Map<String, String> executionParameters) {
		throw new UnsupportedOperationException("Running a plan with execution parameters isn't support by this runner.");
	}
}
