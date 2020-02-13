package step.plugins.parametermanager;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.InMemoryCRUDAccessor;
import step.core.execution.ContextBuilder;
import step.core.execution.ExecutionContext;
import step.functions.Function;

public class ParameterManagerPluginTest {
	
	protected InMemoryCRUDAccessor<Parameter> parameterAccessor = new InMemoryCRUDAccessor<>();
	
	@Test
	public void testEmptyParameterList() {
		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor);
		ExecutionContext executionContext = ContextBuilder.createLocalExecutionContext();
		
		parameterManagerPlugin.executionStart(executionContext);
		
		Function function = newFunction(null, "MyFunction1");
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), function);
	}
	
	@Test
	public void test() {
		// test backward compatibility where the scope is null
		declareParameter("MyOldGlobalParameter", "MyOldGlobalParameterrValue1", null, null);
		
		declareParameter("MyGlobalParameter", "MyGlobalParameterrValue1", ParameterScope.GLOBAL, null);
		
		declareParameter("MyAppParameter1", "MyAppParameterValue2", ParameterScope.APPLICATION, "MyApp");
		
		declareParameter("MyFunctionParameter", "MyFunctionParameterValue1", ParameterScope.FUNCTION, "MyFunction1");
		declareParameter("MyFunctionParameter2", "MyFunctionParameterValue2", ParameterScope.FUNCTION, "MyFunction2");
		
		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor);
		ExecutionContext executionContext = ContextBuilder.createLocalExecutionContext();
		
		parameterManagerPlugin.executionStart(executionContext);
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyGlobalParameter"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyOldGlobalParameter"));
		
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction(null, "MyFunction1"));
		
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter"));
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter2"));
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyAppParameter1"));
		
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction("MyApp", "MyFunction2"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter2"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyAppParameter1"));
	}

	protected void declareParameter(String key, String value, ParameterScope scope, String scopeEntity) {
		Parameter functionParameter = new Parameter();
		functionParameter.setKey(key);
		functionParameter.setValue(value);
		functionParameter.setScope(scope);
		functionParameter.setScopeEntity(scopeEntity);
		parameterAccessor.save(functionParameter);
	}

	protected Function newFunction(String app, String name) {
		Function function = new Function();
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(AbstractOrganizableObject.NAME, name);
		if(app != null) {
			attributes.put(Function.APPLICATION, app);
		}
		function.setAttributes(attributes);
		return function;
	}
	
	public static class LocalParameterManagerPlugin extends ParameterManagerPlugin {

		public LocalParameterManagerPlugin(InMemoryCRUDAccessor<Parameter> parameterAccessor) {
			super();
			parameterManager = new ParameterManager(parameterAccessor);
		}
		
		
	}
}
