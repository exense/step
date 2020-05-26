package step.core.plans.runner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import step.core.artefacts.handlers.ArtefactHandler;
import step.core.execution.ContextBuilder;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.FunctionCRUDAccessor;

/**
 * A simple runner that runs plans locally and doesn't support functions
 * 
 * @author Jérôme Comte
 *
 */
public class DefaultPlanRunner implements PlanRunner {

	private Map<String, String> executionParameters = new HashMap<String, String>();

	@Override
	public PlanRunnerResult run(Plan plan) {
		ExecutionContext context = buildExecutionContext();
		if(plan.getFunctions()!=null) {
			FunctionAccessor functionAccessor = context.get(FunctionAccessor.class);
			if(functionAccessor!=null && functionAccessor instanceof FunctionCRUDAccessor) {
				((FunctionCRUDAccessor)functionAccessor).save(new ArrayList<>(plan.getFunctions()));
			}
		}
		Collection<Plan> subPlans = plan.getSubPlans();
		if(subPlans!=null) {
			PlanAccessor planAccessor = context.getPlanAccessor();
			planAccessor.save(subPlans);
		}
		ArtefactHandler.delegateExecute(context, plan.getRoot(),context.getReport());
		return new PlanRunnerResult(context.getExecutionId(), context.getReport().getId().toString(), context.getReportNodeAccessor());
	}
	
	protected ExecutionContext buildExecutionContext() {
		ExecutionContext context = ContextBuilder.createLocalExecutionContext();
		
		executionParameters.forEach((k, v) -> {
			context.getVariablesManager().putVariable(context.getReport(), k, v);
		});
		return context;
	}

	@Override
	public PlanRunnerResult run(Plan plan, Map<String, String> executionParameters) {
		this.executionParameters = executionParameters;
		return run(plan);
	}
}
