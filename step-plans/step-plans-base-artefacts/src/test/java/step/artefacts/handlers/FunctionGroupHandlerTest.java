package step.artefacts.handlers;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.types.ObjectId;
import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.CallFunction;
import step.artefacts.CheckArtefact;
import step.artefacts.Echo;
import step.artefacts.FunctionGroup;
import step.artefacts.RetryIfFails;
import step.artefacts.Sequence;
import step.artefacts.Sleep;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineException;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.DefaultPlanRunner;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.grid.AgentRef;
import step.grid.Token;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.tokenpool.Interest;

public class FunctionGroupHandlerTest {

	@Test
	public void test() throws IOException, ExecutionEngineException {
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
		ExecutionContext context = newExecutionContext(plan);
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
		DefaultPlanRunner runner = new DefaultPlanRunner(context);
		
		StringWriter writer = new StringWriter();
		runner.run(plan).printTree(writer);
		
		// Assert that the token have been returned after Session execution
		Assert.assertTrue(localTokenReturned.get());
		Assert.assertTrue(tokenReturned.get());
		Assert.assertEquals("Session:PASSED:\n" + 
				" CheckArtefact:PASSED:\n" + 
				" Echo:PASSED:\n" ,writer.toString());
	}

	protected ExecutionContext newExecutionContext(Plan plan) throws ExecutionEngineException {
		return new ExecutionEngine().newExecutionContext(new ExecutionParameters(plan, null));
	}
	
	@Test
	public void testReleaseMultipleErrors() throws IOException, ExecutionEngineException {
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
		
		ExecutionContext context = newExecutionContext(plan);
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
		
		DefaultPlanRunner runner = new DefaultPlanRunner(context);		
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
	public void testReleaseErrors() throws IOException, ExecutionEngineException {
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
		
		ExecutionContext context = newExecutionContext(plan);
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
		
		DefaultPlanRunner runner = new DefaultPlanRunner(context);
		
		StringWriter writer = new StringWriter();
		runner.run(plan).printTree(writer);
		
		// Assert that the token have been returned after Session execution
		Assert.assertTrue(localTokenReturned.get());
		Assert.assertTrue(tokenReturned.get());
		Assert.assertEquals("Session:TECHNICAL_ERROR:Test error\n" + 
				" CheckArtefact:PASSED:\n" + 
				" Echo:PASSED:\n" ,writer.toString());
	}
	
	@Test
	public void testReleaseWaitingArtefacts() throws Exception {
		TokenWrapper localToken = token("local");
		AtomicInteger localTokenReturned = new AtomicInteger();
		
		TokenWrapper token = token("remote");
		AtomicInteger tokenReturned = new AtomicInteger();
		
		Sleep sleepArtefact = new Sleep();
		sleepArtefact.setReleaseTokens(new DynamicValue<Boolean>(true));
		sleepArtefact.setDuration(new DynamicValue<Long>(100L));
		
		Function function = new Function();
		function.setId(new ObjectId());
		CallFunction callFunction = new CallFunction();
		callFunction.setFunctionId(function.getId().toString());

		RetryIfFails retryIfFail = new RetryIfFails();
		retryIfFail.setReleaseTokens(new DynamicValue<Boolean>(true));
		retryIfFail.setMaxRetries(new DynamicValue<Integer>(3));
		retryIfFail.setGracePeriod(new DynamicValue<Integer>(200));		
		Sequence sequence = new Sequence();
		sequence.setContinueOnError(new DynamicValue<Boolean>(true));
		sequence.addChild(sleepArtefact);
		sequence.addChild(callFunction);
		sequence.addChild(retryIfFail);
		sequence.addChild(callFunction);
		
		Plan plan = PlanBuilder.create().startBlock(new FunctionGroup()).add(new CheckArtefact(t-> {
			FunctionGroupContext functionGroupContext = (FunctionGroupContext) t.getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY);
			functionGroupContext.setLocalToken(localToken);
			functionGroupContext.addToken(token);
			t.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
		})).add(sequence).endBlock().build();
		
		ExecutionContext context = newExecutionContext(plan);
		CheckArtefact check1 = new CheckArtefact(c->context.getCurrentReportNode().setStatus(ReportNodeStatus.FAILED));
		retryIfFail.addChild(check1);	
		FunctionTypeRegistry functionTypeRegistry = getFunctionTypeRepository();
		InMemoryFunctionAccessorImpl funcitonAccessor = new InMemoryFunctionAccessorImpl();
		context.put(FunctionAccessor.class, funcitonAccessor);
		((InMemoryFunctionAccessorImpl) context.get(FunctionAccessor.class)).save(function);			
		FunctionExecutionService fctExecSvc = new FunctionExecutionService() {
			@Override
			public TokenWrapper getLocalTokenHandle() {
				return null;
			}

			@Override
			public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests,
					boolean createSession, TokenWrapperOwner tokenWrapperOwner) throws FunctionExecutionServiceException {
				return token("remote");
			}

			@Override
			public void returnTokenHandle(String id) throws FunctionExecutionServiceException {
				if(localToken.getID().equals(id)) {
					localTokenReturned.incrementAndGet();
				}
				if(token.getID().equals(id)) {
					tokenReturned.incrementAndGet();
				}
			}

			@Override
			public <IN, OUT> Output<OUT> callFunction(String id, Function function,
					FunctionInput<IN> input, Class<OUT> outputClass) {
				return null;
			}
			
		};
		context.put(FunctionExecutionService.class, fctExecSvc);
		DefaultFunctionRouterImpl functionRouter = new DefaultFunctionRouterImpl(fctExecSvc, functionTypeRegistry, new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler())));
		context.put(FunctionRouter.class, functionRouter);
		
		DefaultPlanRunner runner = new DefaultPlanRunner(context);
		
		StringWriter writer = new StringWriter();
		runner.run(plan).printTree(writer);
		
		// Assert that the token have been returned after Session execution
		Assert.assertEquals(1,localTokenReturned.get());
		Assert.assertEquals(3,tokenReturned.get());
		Assert.assertEquals("Session:TECHNICAL_ERROR:\n" + 
				" CheckArtefact:PASSED:\n" + 
				 " Sequence:TECHNICAL_ERROR:\n" +
				"  Sleep:PASSED:\n" +
				"  CallKeyword:TECHNICAL_ERROR:null\n" +
				"  RetryIfFails:FAILED:\n" +
				"   Iteration1:FAILED:\n" +
				"    CheckArtefact:FAILED:\n" +
				"   Iteration2:FAILED:\n" +
				"    CheckArtefact:FAILED:\n" +
				"   Iteration3:FAILED:\n" +
				"    CheckArtefact:FAILED:\n" +
				"  CallKeyword:TECHNICAL_ERROR:null\n" ,writer.toString());
	}
	
	protected TokenWrapper token(String id) {
		Token localToken_ = new Token();
		localToken_.setId(id);
		TokenWrapper localToken = new TokenWrapper(localToken_, new AgentRef());
		return localToken;
	}
	
	protected FunctionTypeRegistry getFunctionTypeRepository() {
		return new FunctionTypeRegistry() {
			
			@Override
			public void registerFunctionType(AbstractFunctionType<? extends Function> functionType) {
			}
			
			@Override
			public AbstractFunctionType<Function> getFunctionTypeByFunction(Function function) {
				return dummyFunctionType();
			}
			
			@Override
			public AbstractFunctionType<Function> getFunctionType(String functionType) {
				return dummyFunctionType();
			}

			protected AbstractFunctionType<Function> dummyFunctionType() {
				return new AbstractFunctionType<Function>() {
					
					@Override
					public Function newFunction() {
						return null;
					}
					
					@Override
					public Map<String, String> getHandlerProperties(Function function) {
						return null;
					}
					
					@Override
					public String getHandlerChain(Function function) {
						return null;
					}
				};
			}
		};
	}

}
