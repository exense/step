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
package step.engine.plugins;

import static org.junit.Assert.assertNull;

import java.util.Map;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.planbuilder.FunctionArtefacts;

public class FunctionPluginTest {

	@Test
	public void testNormalMode() {
		AbstractExecutionEngineContext parentContext = new ExecutionEngineContext(OperationMode.LOCAL);
		InMemoryFunctionAccessorImpl functionAccessor = new InMemoryFunctionAccessorImpl();
		
		Function function = new Function();
		functionAccessor.save(function);

		Function function2 = new Function();
		
		parentContext.put(FunctionAccessor.class, functionAccessor);
		
		CustomPlugin plugin = new CustomPlugin(c->{
			// Assert that the function of the parent context is available in the local context
			FunctionAccessor localFunctionAccessor = c.require(FunctionAccessor.class);
			Assert.assertEquals(function, localFunctionAccessor.getAll().next());
			
			localFunctionAccessor.save(function2);
		});
		ExecutionEngine engine = ExecutionEngine.builder().withParentContext(parentContext).withPlugin(new FunctionPlugin()).withPlugin(plugin).build();
		Plan plan = PlanBuilder.create().startBlock(FunctionArtefacts.keyword("My function call")).endBlock().build();
		ExecutionParameters executionParameters = new ExecutionParameters(ExecutionMode.RUN, plan, null, null, null, null, null, true, null);
		engine.execute(executionParameters);
		
		// Assert that the function2 that has been saved to the local function accessor of the execution context
		// has not be saved to the parent context
		Function actual = functionAccessor.get(function2.getId());
		assertNull(actual);
	}
	
	@IgnoreDuringAutoDiscovery
	@Plugin(dependencies = {FunctionPlugin.class})
	public static class CustomPlugin extends AbstractExecutionEnginePlugin {
		
		private final Consumer<ExecutionContext> consumer;
		private CustomFunctionType functionType = new CustomFunctionType();

		public CustomPlugin(Consumer<ExecutionContext> consumer) {
			super();
			this.consumer = consumer;
		}

		@Override
		public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext,
				ExecutionEngineContext executionEngineContext) {
			try {
				FunctionTypeRegistry functionTypeRegistry = executionEngineContext.require(FunctionTypeRegistry.class);
				Assert.assertNotNull(functionTypeRegistry);
				functionTypeRegistry.registerFunctionType(functionType);
				functionType.performAsserts();
			} catch (Throwable e) {
				throw new PluginCriticalException("Error within plugin", e);
			}
		}

		@Override
		public void initializeExecutionContext(ExecutionEngineContext executionEngineContext,
				ExecutionContext executionContext) {
			try {
				FunctionTypeRegistry functionTypeRegistry = executionEngineContext.require(FunctionTypeRegistry.class);
				AbstractFunctionType<Function> functionType = functionTypeRegistry.getFunctionType(Function.class.getName());
				Assert.assertNotNull(functionType);
				Assert.assertTrue(this.functionType == functionType);
				
				consumer.accept(executionContext);

			} catch (Throwable e) {
				throw new PluginCriticalException("Error within plugin", e);
			}
		}
	}
	
	public static class CustomFunctionType extends AbstractFunctionType<Function> {

		@Override
		public String getHandlerChain(Function function) {
			return null;
		}

		@Override
		public Map<String, String> getHandlerProperties(Function function) {
			return null;
		}

		@Override
		public Function newFunction() {
			return new Function();
		}
		
		public void performAsserts() {
			Assert.assertNotNull(this.fileResolver);
			Assert.assertNotNull(this.fileResolverCache);
			Assert.assertNotNull(this.gridFileServices);
			Assert.assertNotNull(this.functionTypeConfiguration);
		}
	}

}
