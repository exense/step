package step.engine.plugins;

import java.util.Map;

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
		ExecutionEngine engine = ExecutionEngine.builder().withPlugin(new FunctionPlugin()).withPlugin(new CustomPlugin()).build();
		engine.execute(PlanBuilder.create().startBlock(FunctionArtefacts.keyword("My function call")).endBlock().build());
	}
	
	@Test
	public void testIsolatedMode() {
		AbstractExecutionEngineContext parentContext = new ExecutionEngineContext(OperationMode.LOCAL);
		InMemoryFunctionAccessorImpl functionAccessor = new InMemoryFunctionAccessorImpl();
		
		Function function = new Function();
		functionAccessor.save(function);
		
		parentContext.put(FunctionAccessor.class, functionAccessor);
		
		ExecutionEngine engine = ExecutionEngine.builder().withParentContext(parentContext).withPlugin(new FunctionPlugin()).withPlugin(new CustomPlugin()).build();
		Plan plan = PlanBuilder.create().startBlock(FunctionArtefacts.keyword("My function call")).endBlock().build();
		ExecutionParameters executionParameters = new ExecutionParameters(ExecutionMode.RUN, plan, null, null, null, null, null, true, null);
		engine.execute(executionParameters);
	}
	
	@Plugin(dependencies = {FunctionPlugin.class})
	public static class CustomPlugin extends AbstractExecutionEnginePlugin {
		
		private CustomFunctionType functionType = new CustomFunctionType();

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
				
				
				FunctionAccessor functionAccessor = executionContext.require(FunctionAccessor.class);
				// Assert that the function accessor contains no entry.
				// This assert is performed for the test testIsolatedMode where the isolation mode should guaranty that 
				// a new function accessor is created for the scope of the execution
				Assert.assertFalse(functionAccessor.getAll().hasNext());
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
