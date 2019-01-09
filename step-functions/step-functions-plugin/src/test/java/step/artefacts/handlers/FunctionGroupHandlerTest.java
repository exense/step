package step.artefacts.handlers;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.CheckArtefact;
import step.artefacts.Echo;
import step.artefacts.FunctionGroup;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.DefaultPlanRunner;
import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.io.Input;
import step.functions.io.Output;
import step.grid.TokenWrapper;
import step.grid.tokenpool.Interest;

public class FunctionGroupHandlerTest {

	@Test
	public void test() throws IOException {
		TokenWrapper localToken = new TokenWrapper();
		AtomicBoolean localTokenReturned = new AtomicBoolean(false);
		
		TokenWrapper token = new TokenWrapper();
		AtomicBoolean tokenReturned = new AtomicBoolean(false);
		
		Plan plan = PlanBuilder.create().startBlock(new FunctionGroup()).add(new CheckArtefact(t-> {
			FunctionGroupContext functionGroupContext = (FunctionGroupContext) t.getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY);
			functionGroupContext.setLocalToken(localToken);
			functionGroupContext.setToken(token);
			t.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
		})).add(new Echo()).endBlock().build();
		DefaultPlanRunner runner = new DefaultPlanRunner() {

			@Override
			protected ExecutionContext buildExecutionContext() {
				ExecutionContext context = super.buildExecutionContext();
				context.put(FunctionExecutionService.class, new FunctionExecutionService() {

					@Override
					public TokenWrapper getLocalTokenHandle() {
						return null;
					}

					@Override
					public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests,
							boolean createSession) throws FunctionExecutionServiceException {
						return null;
					}

					@Override
					public void returnTokenHandle(TokenWrapper adapterToken) throws FunctionExecutionServiceException {
						if(adapterToken == localToken) {
							localTokenReturned.set(true);
						}
						if(adapterToken == token) {
							tokenReturned.set(true);
						}
					}

					@Override
					public <IN, OUT> Output<OUT> callFunction(TokenWrapper tokenHandle, Function function,
							Input<IN> input, Class<OUT> outputClass) {
						return null;
					}
					
				});
				return context;
			}
			
		};
		
		StringWriter writer = new StringWriter();
		runner.run(plan).printTree(writer);
		
		// Assert that the token have been returned after Session execution
		Assert.assertTrue(localTokenReturned.get());
		Assert.assertTrue(tokenReturned.get());
		Assert.assertEquals("Session:PASSED:\n" + 
				" CheckArtefact:PASSED:\n" + 
				" Echo:PASSED:\n" ,writer.toString());
	}

}
