package step.core.plans.runner;

import java.util.ArrayList;

import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ContextBuilder;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;

public class DefaultPlanRunner implements PlanRunner {
	
	ExecutionContext context;
	
	protected void init() {
		context = ContextBuilder.createLocalExecutionContext();
	}

	@Override
	public ReportNode run(Plan plan) {
		init();
		context.getArtefactAccessor().save(new ArrayList<>(plan.getArtefacts()));
		return ArtefactHandler.delegateExecute(context, plan.getRoot(),context.getReport());
	}
}
