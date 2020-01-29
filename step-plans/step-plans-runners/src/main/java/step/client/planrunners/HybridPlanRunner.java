package step.client.planrunners;

import java.util.Map;

import step.artefacts.handlers.DefaultFunctionRouterImpl;
import step.artefacts.handlers.FunctionRouter;
import step.client.accessors.RemoteFunctionAccessorImpl;
import step.client.credentials.ControllerCredentials;
import step.client.credentials.SyspropCredendialsBuilder;
import step.client.functions.RemoteFunctionExecutionService;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ContextBuilder;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.plans.runner.PlanRunner;
import step.core.plans.runner.PlanRunnerResult;
import step.functions.accessor.FunctionAccessor;
import step.functions.execution.FunctionExecutionService;

/**
 * A runner that runs plans locally but functions remotely
 * 
 * @author Jérôme Comte
 *
 */
public class HybridPlanRunner implements PlanRunner {

	protected ControllerCredentials credentials;
	
	// Default = Sysprop for build
	public HybridPlanRunner(){
		this(SyspropCredendialsBuilder.build());
	}
	
	public HybridPlanRunner(ControllerCredentials credentials) {
		super();
		this.credentials = credentials;
	}

	@Override
	public PlanRunnerResult run(Plan plan) {
		ExecutionContext context = ContextBuilder.createLocalExecutionContext();
		
		FunctionAccessor functionAccessor = new RemoteFunctionAccessorImpl(credentials);
		FunctionExecutionService functionExecutionService = new RemoteFunctionExecutionService(credentials);
		
		context.put(FunctionAccessor.class, functionAccessor);
		context.put(FunctionExecutionService.class, functionExecutionService);
		context.put(FunctionRouter.class, new DefaultFunctionRouterImpl(functionExecutionService, null, new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()))));
		ArtefactHandler.delegateExecute(context, plan.getRoot(),context.getReport());
		
		return new PlanRunnerResult(context.getExecutionId(), context.getReport().getId().toString(), context.getReportNodeAccessor());
	}

	@Override
	public PlanRunnerResult run(Plan plan, Map<String, String> executionParameters) {
		throw new UnsupportedOperationException("Running a plan with execution parameters isn't support by this runner.");
	}
}
