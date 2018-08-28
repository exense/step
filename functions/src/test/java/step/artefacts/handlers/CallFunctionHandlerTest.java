/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.artefacts.handlers;

import java.util.Map;

import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.CallFunction;
import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ContextBuilder;
import step.core.execution.ExecutionContext;
import step.functions.Function;
import step.functions.Input;
import step.functions.Output;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.execution.FunctionExecutionService;
import step.functions.routing.FunctionRouter;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.grid.AgentRef;
import step.grid.Token;
import step.grid.TokenWrapper;
import step.grid.client.GridClient.AgentCommunicationException;
import step.grid.tokenpool.Interest;

public class CallFunctionHandlerTest {
	
	@Test
	public void test() {
		ExecutionContext executionContext = ContextBuilder.createLocalExecutionContext();

		Function function = new Function();
		
		InMemoryFunctionAccessorImpl funcitonAccessor = new InMemoryFunctionAccessorImpl();
		funcitonAccessor.save(function);
		
		FunctionTypeRegistry functionTypeRegistry = new FunctionTypeRegistry() {
			
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
		
		Token localToken = new Token();
		localToken.setAgentid("local");
		TokenWrapper token = new TokenWrapper();
		token.setToken(localToken);
		token.setAgent(new AgentRef());
		
		FunctionExecutionService functionExecutionService = new FunctionExecutionService() {
			
			@Override
			public void returnTokenHandle(TokenWrapper adapterToken) throws AgentCommunicationException {
				
			}
			
			@Override
			public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests,
					boolean createSession) throws AgentCommunicationException {
				return token;
			}
			
			@Override
			public TokenWrapper getLocalTokenHandle() {
				return null;
			}
			
			@Override
			public Output callFunction(TokenWrapper tokenHandle, String functionId, Input input) {
				return new Output();
			}
			
			@Override
			public Output callFunction(TokenWrapper tokenHandle, Map<String, String> functionAttributes, Input input) {
				return new Output();
			}
		};
		
		FunctionRouter functionRouter = new FunctionRouter(functionExecutionService, functionTypeRegistry, new DynamicJsonObjectResolver(new DynamicJsonValueResolver(executionContext.getExpressionHandler())));
		
		executionContext.put(FunctionAccessor.class, funcitonAccessor);
		executionContext.put(FunctionRouter.class, functionRouter);
		executionContext.put(FunctionExecutionService.class, functionExecutionService);
		
		CallFunctionHandler handler = new CallFunctionHandler();
		handler.init(executionContext);
		
		CallFunction callFunction = new CallFunction();
		callFunction.setFunctionId(function.getId().toString());
		
		CallFunctionReportNode node = (CallFunctionReportNode) ArtefactHandler.delegateExecute(executionContext, callFunction, new ReportNode());
		
		Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
	}
}

