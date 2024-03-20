/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.artefacts.handlers;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.Test;
import step.artefacts.*;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.core.AbstractStepContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.ExecutionEngineException;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.execution.TokenLifecycleInterceptor;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.grid.AgentRef;
import step.grid.Token;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.tokenpool.Interest;
import step.planbuilder.FunctionArtefacts;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FunctionGroupHandlerTest {

	@Test
	public void test() throws IOException, ExecutionEngineException {
		TokenWrapper localToken = token("local");
		AtomicBoolean localTokenReturned = new AtomicBoolean(false);
		
		TokenWrapper token = token("remote");
		AtomicBoolean tokenReturned = new AtomicBoolean(false);
		
		Plan plan = PlanBuilder.create().startBlock(new FunctionGroup()).add(new CheckArtefact(t-> {
			FunctionGroupContext functionGroupContext = (FunctionGroupContext) t.getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY);
			//functionGroupContext.setLocalToken(localToken);
			//functionGroupContext.addToken(token);
			t.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
		})).add(new Echo()).endBlock().build();

		StringWriter writer = new StringWriter();
		try (ExecutionEngine engine = newEngineWithCustomTokenReleaseFunction(id -> {
			if (localToken.getID().equals(id)) {
				localTokenReturned.set(true);
			}
			if (token.getID().equals(id)) {
				tokenReturned.set(true);
			}
		})) {
			engine.execute(plan).printTree(writer);
		}

		// Assert that the token have been returned after Session execution
		assertTrue(localTokenReturned.get());
		assertTrue(tokenReturned.get());
		assertEquals("Session:PASSED:\n" + 
				" CheckArtefact:PASSED:\n" + 
				" Echo:PASSED:\n" ,writer.toString());
	}
	
	private ExecutionEngine newEngineWithCustomTokenReleaseFunction(Consumer<String> tokenReleaseFunction) {
		return ExecutionEngine.builder().withPlugin(new BaseArtefactPlugin()).withPlugin(new AbstractExecutionEnginePlugin() {

			@Override
			public void initializeExecutionContext(ExecutionEngineContext executionEngineContext,
					ExecutionContext executionContext) {
				executionContext.put(FunctionExecutionService.class, new FunctionExecutionService() {

					@Override
					public void registerTokenLifecycleInterceptor(TokenLifecycleInterceptor interceptor) {

					}

					@Override
					public void unregisterTokenLifecycleInterceptor(TokenLifecycleInterceptor interceptor) {

					}

					@Override
					public TokenWrapper getLocalTokenHandle() {
						return null;
					}

					@Override
					public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests,
							boolean createSession, TokenWrapperOwner tokenWrapperOwner) {
						return null;
					}

					@Override
					public void returnTokenHandle(String id) throws FunctionExecutionServiceException {
						try {
							tokenReleaseFunction.accept(id);
						} catch(Exception e) {
							throw new FunctionExecutionServiceException(e.getMessage());
						}
					}

					@Override
					public <IN, OUT> Output<OUT> callFunction(String id, Function function,
							FunctionInput<IN> input, Class<OUT> outputClass) {
						return (Output<OUT>) newOutput();
					}
					private Output<JsonObject> newOutput() {
						Output<JsonObject> output = new Output<>();
						JsonObject payload = Json.createObjectBuilder().build();
						output.setPayload(payload);
						output.setMeasures(new ArrayList<>());
						output.setAttachments(new ArrayList<>());
						return output;
					}
				});
			}
		}).build();
	}
	
	@Test
	public void testReleaseMultipleErrors() throws IOException, ExecutionEngineException {
		TokenWrapper localToken = token("local");
		AtomicBoolean localTokenReturned = new AtomicBoolean(false);
		
		TokenWrapper token = token("remote");
		AtomicBoolean tokenReturned = new AtomicBoolean(false);
		
		Plan plan = PlanBuilder.create().startBlock(new FunctionGroup()).add(new CheckArtefact(t-> {
			FunctionGroupContext functionGroupContext = (FunctionGroupContext) t.getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY);
			//functionGroupContext.setLocalToken(localToken);
			//functionGroupContext.addToken(token);
			t.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
		})).add(new Echo()).endBlock().build();

		StringWriter writer = new StringWriter();
		try (ExecutionEngine engine = newEngineWithCustomTokenReleaseFunction(id -> {
			if (localToken.getID().equals(id)) {
				localTokenReturned.set(true);
			}
			if (token.getID().equals(id)) {
				tokenReturned.set(true);
			}
			throw new RuntimeException("Test error");
		})) {
			engine.execute(plan).printTree(writer);
		}

		// Assert that the token have been returned after Session execution
		assertTrue(localTokenReturned.get());
		assertTrue(tokenReturned.get());
		assertEquals("Session:TECHNICAL_ERROR:Multiple errors occurred when releasing agent tokens: Test error, Test error\n" + 
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
			//functionGroupContext.setLocalToken(localToken);
			//functionGroupContext.addToken(token);
			t.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
		})).add(new Echo()).endBlock().build();

		StringWriter writer = new StringWriter();
		try (ExecutionEngine engine = newEngineWithCustomTokenReleaseFunction(id -> {
			if (localToken.getID().equals(id)) {
				localTokenReturned.set(true);
			}
			if (token.getID().equals(id)) {
				tokenReturned.set(true);
				throw new RuntimeException("Test error");
			}
		})) {
			engine.execute(plan).printTree(writer);
		}

		// Assert that the token have been returned after Session execution
		assertTrue(localTokenReturned.get());
		assertTrue(tokenReturned.get());
		assertEquals("Session:TECHNICAL_ERROR:Test error\n" + 
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
		sleepArtefact.setReleaseTokens(new DynamicValue<>(true));
		sleepArtefact.setDuration(new DynamicValue<>(100L));
		
		Function function = new Function();
		String name = UUID.randomUUID().toString();
		function.addAttribute(AbstractOrganizableObject.NAME, name);
		CallFunction callFunction = FunctionArtefacts.keyword(name);

		RetryIfFails retryIfFail = new RetryIfFails();
		retryIfFail.setReleaseTokens(new DynamicValue<>(true));
		retryIfFail.setMaxRetries(new DynamicValue<>(3));
		retryIfFail.setGracePeriod(new DynamicValue<>(200));
		Sequence sequence = new Sequence();
		sequence.setContinueOnError(new DynamicValue<>(true));
		sequence.addChild(sleepArtefact);
		sequence.addChild(callFunction);
		sequence.addChild(retryIfFail);
		sequence.addChild(callFunction);
		
		Plan plan = PlanBuilder.create().startBlock(new FunctionGroup()).add(new CheckArtefact(t-> {
			FunctionGroupContext functionGroupContext = (FunctionGroupContext) t.getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY);
			//functionGroupContext.setLocalToken(localToken);
			//functionGroupContext.addToken(token);
			t.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
		})).add(sequence).endBlock().build();
		
		CheckArtefact check1 = new CheckArtefact(c->c.getCurrentReportNode().setStatus(ReportNodeStatus.FAILED));
		retryIfFail.addChild(check1);	
		InMemoryFunctionAccessorImpl functionAccessor = new InMemoryFunctionAccessorImpl();
		functionAccessor.save(function);

		ExecutionEngine engine = ExecutionEngine.builder().withPlugin(new BaseArtefactPlugin()).withPlugin(new AbstractExecutionEnginePlugin() {

			@Override
			public void initializeExecutionContext(ExecutionEngineContext executionEngineContext,
					ExecutionContext executionContext) {
				FunctionTypeRegistry functionTypeRegistry = getFunctionTypeRepository();

				executionContext.put(FunctionAccessor.class, functionAccessor);
				FunctionExecutionService functionExecutionService = new FunctionExecutionService() {

					@Override
					public void registerTokenLifecycleInterceptor(TokenLifecycleInterceptor interceptor) {

					}

					@Override
					public void unregisterTokenLifecycleInterceptor(TokenLifecycleInterceptor interceptor) {

					}

					@Override
							public TokenWrapper getLocalTokenHandle() {
								return null;
							}

							@Override
							public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests,
									boolean createSession, TokenWrapperOwner tokenWrapperOwner) {
								return token("remote");
							}

							@Override
							public void returnTokenHandle(String id) {
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
								return (Output<OUT>) newOutput();
							}
							private Output<JsonObject> newOutput() {
								Output<JsonObject> output = new Output<>();
								JsonObject payload = Json.createObjectBuilder().build();
								output.setPayload(payload);
								output.setMeasures(new ArrayList<>());
								output.setAttachments(new ArrayList<>());
								return output;
							}

						};
				executionContext.put(FunctionExecutionService.class, functionExecutionService);
			}
		}).build();

		StringWriter writer = new StringWriter();
		engine.execute(plan).printTree(writer);

		// Assert that the token have been returned after Session execution
		assertEquals(1,localTokenReturned.get());
		assertEquals(3,tokenReturned.get());
		assertEquals(("Session:FAILED:\n" +
				" CheckArtefact:PASSED:\n" + 
				" Sequence:FAILED:\n" +
				"  Sleep:PASSED:\n" +
				"  CallKeyword:PASSED:\n" +
				"  RetryIfFails:FAILED:\n" +
				"   Iteration1:FAILED:\n" +
				"    CheckArtefact:FAILED:\n" +
				"   Iteration2:FAILED:\n" +
				"    CheckArtefact:FAILED:\n" +
				"   Iteration3:FAILED:\n" +
				"    CheckArtefact:FAILED:\n" +
				"  CallKeyword:PASSED:\n").replace("CallKeyword", name) ,writer.toString());
	}
	
	protected TokenWrapper token(String id) {
		Token localToken_ = new Token();
		localToken_.setId(id);
		localToken_.setAgentid(id);
		return new TokenWrapper(localToken_, new AgentRef());
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

			private AbstractFunctionType<Function> dummyFunctionType() {
				return new AbstractFunctionType<>() {

					@Override
					public Function newFunction() {
						return null;
					}

					@Override
					public Map<String, String> getHandlerProperties(Function function, AbstractStepContext executionContext) {
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
