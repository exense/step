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
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.grid.AgentRef;
import step.grid.Token;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.tokenpool.Interest;

public class FunctionGroupHandlerTest {

	@Test
	public void test() throws IOException {
		TokenWrapper localToken = token("local");
		AtomicBoolean localTokenReturned = new AtomicBoolean(false);
		
		TokenWrapper token = token("remote");
		AtomicBoolean tokenReturned = new AtomicBoolean(false);
		
		Plan plan = PlanBuilder.create().startBlock(new FunctionGroup()).add(new CheckArtefact(t-> {
			FunctionGroupContext functionGroupContext = (FunctionGroupContext) t.getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY);
			functionGroupContext.setLocalToken(localToken);
			functionGroupContext.addToken(token);
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
							boolean createSession, TokenWrapperOwner tokenWrapperOwner) throws FunctionExecutionServiceException {
						return null;
					}

					@Override
					public void returnTokenHandle(String id) throws FunctionExecutionServiceException {
						if(localToken.getID().equals(id)) {
							localTokenReturned.set(true);
						}
						if(token.getID().equals(id)) {
							tokenReturned.set(true);
						}
					}

					@Override
					public <IN, OUT> Output<OUT> callFunction(String id, Function function,
							FunctionInput<IN> input, Class<OUT> outputClass) {
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
	
	@Test
	public void testReleaseMultipleErrors() throws IOException {
		TokenWrapper localToken = token("local");
		AtomicBoolean localTokenReturned = new AtomicBoolean(false);
		
		TokenWrapper token = token("remote");
		AtomicBoolean tokenReturned = new AtomicBoolean(false);
		
		Plan plan = PlanBuilder.create().startBlock(new FunctionGroup()).add(new CheckArtefact(t-> {
			FunctionGroupContext functionGroupContext = (FunctionGroupContext) t.getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY);
			functionGroupContext.setLocalToken(localToken);
			functionGroupContext.addToken(token);
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
							boolean createSession, TokenWrapperOwner tokenWrapperOwner) throws FunctionExecutionServiceException {
						return null;
					}

					@Override
					public void returnTokenHandle(String id) throws FunctionExecutionServiceException {
						if(localToken.getID().equals(id)) {
							localTokenReturned.set(true);
						}
						if(token.getID().equals(id)) {
							tokenReturned.set(true);
						}
						throw new FunctionExecutionServiceException("Test error");
					}

					@Override
					public <IN, OUT> Output<OUT> callFunction(String id, Function function,
							FunctionInput<IN> input, Class<OUT> outputClass) {
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
		Assert.assertEquals("Session:TECHNICAL_ERROR:Multiple errors occurred when releasing agent tokens: Test error, Test error\n" + 
				" CheckArtefact:PASSED:\n" + 
				" Echo:PASSED:\n" ,writer.toString());
	}
	
	@Test
	public void testReleaseErrors() throws IOException {
		TokenWrapper localToken = token("local");
		AtomicBoolean localTokenReturned = new AtomicBoolean(false);
		
		TokenWrapper token = token("remote");
		AtomicBoolean tokenReturned = new AtomicBoolean(false);
		
		Plan plan = PlanBuilder.create().startBlock(new FunctionGroup()).add(new CheckArtefact(t-> {
			FunctionGroupContext functionGroupContext = (FunctionGroupContext) t.getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY);
			functionGroupContext.setLocalToken(localToken);
			functionGroupContext.addToken(token);
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
							boolean createSession, TokenWrapperOwner tokenWrapperOwner) throws FunctionExecutionServiceException {
						return null;
					}

					@Override
					public void returnTokenHandle(String id) throws FunctionExecutionServiceException {
						if(localToken.getID().equals(id)) {
							localTokenReturned.set(true);
						}
						if(token.getID().equals(id)) {
							tokenReturned.set(true);
							throw new FunctionExecutionServiceException("Test error");
						}
					}

					@Override
					public <IN, OUT> Output<OUT> callFunction(String id, Function function,
							FunctionInput<IN> input, Class<OUT> outputClass) {
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
		Assert.assertEquals("Session:TECHNICAL_ERROR:Test error\n" + 
				" CheckArtefact:PASSED:\n" + 
				" Echo:PASSED:\n" ,writer.toString());
	}

	protected TokenWrapper token(String id) {
		Token localToken_ = new Token();
		localToken_.setId(id);
		TokenWrapper localToken = new TokenWrapper(localToken_, new AgentRef());
		return localToken;
	}

}
