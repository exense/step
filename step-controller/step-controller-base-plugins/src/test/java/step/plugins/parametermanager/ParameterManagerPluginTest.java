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
package step.plugins.parametermanager;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import step.parameter.Parameter;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.InMemoryCRUDAccessor;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.functions.Function;
import step.parameter.ParameterManager;
import step.parameter.ParameterScope;

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
