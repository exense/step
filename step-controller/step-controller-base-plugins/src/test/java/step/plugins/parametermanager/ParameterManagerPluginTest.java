package step.plugins.parametermanager;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.InMemoryCRUDAccessor;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.functions.Function;

public class ParameterManagerPluginTest {
	
	protected InMemoryCRUDAccessor<Parameter> parameterAccessor = new InMemoryCRUDAccessor<>();
	
	@Test
	public void testEmptyParameterList() {
		ExecutionContext executionContext = newExecutionContext();
		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, executionContext);
		
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
		
		declareParameter("MyApp.MyFunctionParameter3", "MyApp.MyFunctionParameter3Value1", ParameterScope.FUNCTION, "MyApp.MyFunction3");
		declareParameter("MyFunctionParameter3", "MyFunctionParameter3Value1", ParameterScope.FUNCTION, "MyFunction3");

		ExecutionContext executionContext = newExecutionContext();
		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, executionContext);
		parameterManagerPlugin.executionStart(executionContext);
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyGlobalParameter"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyOldGlobalParameter"));
		
		executionContext = newExecutionContext();
		parameterManagerPlugin.executionStart(executionContext);
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction(null, "MyFunction1"));
		
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter"));
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter2"));
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyAppParameter1"));
		
		executionContext = newExecutionContext();
		parameterManagerPlugin.executionStart(executionContext);
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction("MyApp", "MyFunction2"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter2"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyAppParameter1"));
		
		executionContext = newExecutionContext();
		parameterManagerPlugin.executionStart(executionContext);
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction("MyApp", "MyFunction3"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyApp.MyFunctionParameter3"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter3"));
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter1"));
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter2"));
	}

	protected ExecutionContext newExecutionContext() {
		return ExecutionEngine.builder().build().newExecutionContext();
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
	
	@IgnoreDuringAutoDiscovery
	public static class LocalParameterManagerPlugin extends ParameterManagerPlugin {

		public LocalParameterManagerPlugin(InMemoryCRUDAccessor<Parameter> parameterAccessor, ExecutionContext executionContext) {
			super(new ParameterManager(parameterAccessor, executionContext.getConfiguration()));
		}
		
		
	}
}
