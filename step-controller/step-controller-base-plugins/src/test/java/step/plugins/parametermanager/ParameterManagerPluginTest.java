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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import ch.exense.commons.app.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.InMemoryAccessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.encryption.EncryptionManager;
import step.core.encryption.EncryptionManagerException;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.exceptions.PluginCriticalException;
import step.core.variables.VariablesManager;
import step.engine.plugins.ExecutionEnginePlugin;
import step.expressions.ExpressionHandler;
import step.expressions.ProtectedBinding;
import step.functions.Function;
import step.parameter.Parameter;
import step.parameter.ParameterManager;
import step.parameter.ParameterScope;

public class ParameterManagerPluginTest {
	
	protected InMemoryAccessor<Parameter> parameterAccessor = new InMemoryAccessor<>();
	private EncryptionManager encryptionManager;
	private EncryptionManager errorEncryptionManager;
	
	@Before
	public void before() {
		encryptionManager = new EncryptionManager() {
			
			@Override
			public String encrypt(String value) throws EncryptionManagerException {
				return "###"+value;
			}
			
			@Override
			public String decrypt(String encryptedValue) throws EncryptionManagerException {
				return encryptedValue.replaceFirst("###", "");
			}

			@Override
			public boolean isKeyPairChanged() {
				return false;
			}

			@Override
			public boolean isFirstStart() {
				return false;
			}
		};
		errorEncryptionManager = new EncryptionManager() {
			
			@Override
			public String encrypt(String value) throws EncryptionManagerException {
				return null;
			}
			
			@Override
			public String decrypt(String encryptedValue) throws EncryptionManagerException {
				throw new EncryptionManagerException("Error");
			}

			@Override
			public boolean isKeyPairChanged() {
				return false;
			}

			@Override
			public boolean isFirstStart() {
				return false;
			}
		};
	}
	
	@Test
	public void testEmptyParameterList() {
		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, new Configuration());
		ExecutionContext executionContext = newExecutionContext(parameterManagerPlugin);

		
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


		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, new Configuration());
		ExecutionContext executionContext = newExecutionContext(parameterManagerPlugin);
		parameterManagerPlugin.executionStart(executionContext);
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyGlobalParameter"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyOldGlobalParameter"));
		
		executionContext = newExecutionContext(parameterManagerPlugin);
		parameterManagerPlugin.executionStart(executionContext);
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction(null, "MyFunction1"));
		
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter"));
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter2"));
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyAppParameter1"));
		
		executionContext = newExecutionContext(parameterManagerPlugin);
		parameterManagerPlugin.executionStart(executionContext);
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction("MyApp", "MyFunction2"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter2"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyAppParameter1"));
		
		executionContext = newExecutionContext(parameterManagerPlugin);
		parameterManagerPlugin.executionStart(executionContext);
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction("MyApp", "MyFunction3"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyApp.MyFunctionParameter3"));
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter3"));
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter1"));
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter2"));
	}
	
	@Test
	public void testProtectedParameters() {
		declareProtectedParameter("MyProtectedParameterWithoutScope", "Value", null, null);
		declareProtectedParameter("MyProtectedParameterWithGlobalScope", "Value", ParameterScope.GLOBAL, null);
		declareProtectedParameter("MyProtectedParameterWithOtherApplicationScope", "Value", ParameterScope.APPLICATION, "MyOtherApp");
		declareProtectedParameter("MyProtectedParameterWithApplicationScope", "Value", ParameterScope.APPLICATION, "MyApp");
		declareProtectedParameter("MyProtectedParameterWithFunctionScope", "Value", ParameterScope.FUNCTION, "MyFunction1");

		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, encryptionManager, new Configuration());
		ExecutionContext executionContext = newExecutionContext(parameterManagerPlugin);
		VariablesManager variablesManager = executionContext.getVariablesManager();
		parameterManagerPlugin.executionStart(executionContext);
		// The parameters should be available as protected binding
		Assert.assertEquals("***MyProtectedParameterWithoutScope***", variablesManager.getVariable("MyProtectedParameterWithoutScope").toString());
		Assert.assertEquals("***MyProtectedParameterWithGlobalScope***", variablesManager.getVariable("MyProtectedParameterWithGlobalScope").toString());
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithApplicationScope"));
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithFunctionScope"));

		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction("MyApp", "MyFunction1"));

		// All parameters should now be available and their value should be decrypted
		assertProtectedVariable(variablesManager, "MyProtectedParameterWithoutScope");
		assertProtectedVariable(variablesManager, "MyProtectedParameterWithGlobalScope");
		assertProtectedVariable(variablesManager, "MyProtectedParameterWithApplicationScope");
		assertProtectedVariable(variablesManager, "MyProtectedParameterWithFunctionScope");
	}
	
	@Test
	public void testProtectedParametersWithoutEncryptionManager() {
		declareProtectedParameter("MyProtectedParameterWithoutScope", "Value", null, null);
		declareProtectedParameter("MyProtectedParameterWithGlobalScope", "Value", ParameterScope.GLOBAL, null);
		declareProtectedParameter("MyProtectedParameterWithApplicationScope", "Value", ParameterScope.APPLICATION, "MyApp");
		declareProtectedParameter("MyProtectedParameterWithFunctionScope", "Value", ParameterScope.FUNCTION, "MyFunction1");

		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, new Configuration());
		ExecutionContext executionContext = newExecutionContext(parameterManagerPlugin);
		VariablesManager variablesManager = executionContext.getVariablesManager();
		parameterManagerPlugin.executionStart(executionContext);
		// The global protected parameters should be available as no encryption manager is active
		Assert.assertNotNull(variablesManager.getVariable("MyProtectedParameterWithoutScope"));
		Assert.assertNotNull(variablesManager.getVariable("MyProtectedParameterWithGlobalScope"));
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithApplicationScope"));
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithFunctionScope"));
		
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction(null, "MyFunction1"));
		// All parameters should now be available as they are all protected
		Assert.assertNotNull(variablesManager.getVariable("MyProtectedParameterWithoutScope"));
		Assert.assertNotNull(variablesManager.getVariable("MyProtectedParameterWithGlobalScope"));
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithApplicationScope"));
		Assert.assertNotNull(variablesManager.getVariable("MyProtectedParameterWithFunctionScope"));
	}
	
	@Test
	public void testProtectedAndEncryptedParameters() {
		declareProtectedAndEncryptedParameter("MyProtectedParameterWithoutScope", "Value", null, null);
		declareProtectedAndEncryptedParameter("MyProtectedParameterWithGlobalScope", "Value", ParameterScope.GLOBAL, null);
		declareProtectedAndEncryptedParameter("MyProtectedParameterWithApplicationScope", "Value", ParameterScope.APPLICATION, "MyApp");
		declareProtectedAndEncryptedParameter("MyProtectedParameterWithFunctionScope", "Value", ParameterScope.FUNCTION, "MyFunction1");

		// Construct a ParameterManagerPlugin with encryption manager
		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, encryptionManager, new Configuration());
		ExecutionContext executionContext = newExecutionContext(parameterManagerPlugin);
		VariablesManager variablesManager = executionContext.getVariablesManager();

		parameterManagerPlugin.executionStart(executionContext);
		// The parameters should be available as protected binding
		Assert.assertEquals("***MyProtectedParameterWithoutScope***", variablesManager.getVariable("MyProtectedParameterWithoutScope").toString());
		Assert.assertEquals("***MyProtectedParameterWithGlobalScope***", variablesManager.getVariable("MyProtectedParameterWithGlobalScope").toString());
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithApplicationScope"));
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithFunctionScope"));
		
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction("MyApp", "MyFunction1"));

		// All parameters should now be available and their value should be decrypted
		assertProtectedVariable(variablesManager, "MyProtectedParameterWithoutScope");
		assertProtectedVariable(variablesManager, "MyProtectedParameterWithGlobalScope");
		assertProtectedVariable(variablesManager, "MyProtectedParameterWithApplicationScope");
		assertProtectedVariable(variablesManager, "MyProtectedParameterWithFunctionScope");
	}
	
	@Test
	public void testEncryptedParametersAndNoEncryptionManagerAvailable() {
		declareProtectedAndEncryptedParameter("MyProtectedParameterWithoutScope", "Value", null, null);



		// Construct a ParameterManagerPlugin without encryption manager
		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, null, new Configuration());
		ExecutionContext executionContext = newExecutionContext(parameterManagerPlugin);

		Exception actualException = null;
		try {
			parameterManagerPlugin.executionStart(executionContext);
		} catch (Exception e) {
			actualException = e;
		}
		assertNotNull(actualException);
		assertEquals(
				"Unable to decrypt value of parameter MyProtectedParameterWithoutScope. No encryption manager available",
				actualException.getMessage());
	}
	
	@Test
	public void testEncryptedParametersAndErrorEncryptionManager() {
		declareProtectedAndEncryptedParameter("MyProtectedParameterWithoutScope", "Value", ParameterScope.APPLICATION, "MyApp1");

		// Construct ParameterManagerPlugin with error encryption manager (failing on decrypt())
		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, errorEncryptionManager, new Configuration());
		ExecutionContext executionContext = newExecutionContext(parameterManagerPlugin);

		Exception actualException = null;
		try {
			parameterManagerPlugin.executionStart(executionContext);
		} catch (Exception e) {
			actualException = e;
		}
		assertNotNull(actualException);
		assertEquals(
				"Error while decrypting value of parameter MyProtectedParameterWithoutScope",
				actualException.getMessage());
	}

	@Test
	public void testDynamicValues() {
		// test backward compatibility where the scope is null
		declareParameter("MyOldGlobalParameter", "MyOldGlobalParameterrValue1", null, null);

		declareDynamicParameter("MyGlobalParameter", "\"${MyOldGlobalParameter}_MyGlobalParameterrValue1\"", ParameterScope.GLOBAL, null);

		declareDynamicParameter("MyAppParameter1", "\"${MyGlobalParameter}_MyAppParameterValue2\"", ParameterScope.APPLICATION, "MyApp");

		declareDynamicParameter("MyFunctionParameter", "\"MyFunctionParameterValue1\"", ParameterScope.FUNCTION, "MyFunction1");
		declareDynamicParameter("MyFunctionParameterComposed", "\"${MyFunctionParameter}_MyFunctionParameterValue2\"", ParameterScope.FUNCTION, "MyFunction1");
		declareParameter("MyFunctionParameter2", "MyFunctionParameterValue2", ParameterScope.FUNCTION, "MyFunction2");

		declareDynamicParameter("MyApp.MyFunctionParameter3", "\"MyApp.MyFunctionParameter3Value1\"", ParameterScope.FUNCTION, "MyApp.MyFunction3");
		declareDynamicParameter("MyFunctionParameter3", "\"MyFunctionParameter3Value1\"", ParameterScope.FUNCTION, "MyFunction3");


		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, new Configuration());
		ExecutionContext executionContext = newExecutionContext(parameterManagerPlugin);
		parameterManagerPlugin.executionStart(executionContext);
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter"));
		String myGlobalParameter = (String) executionContext.getVariablesManager().getVariable("MyGlobalParameter");
		Assert.assertNotNull(myGlobalParameter);
		Assert.assertEquals("MyOldGlobalParameterrValue1_MyGlobalParameterrValue1", myGlobalParameter);
		Assert.assertNotNull(executionContext.getVariablesManager().getVariable("MyOldGlobalParameter"));

		executionContext = newExecutionContext(parameterManagerPlugin);
		parameterManagerPlugin.executionStart(executionContext);
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction(null, "MyFunction1"));

		String myFunctionParameter = (String) executionContext.getVariablesManager().getVariable("MyFunctionParameter");
		Assert.assertNotNull(myFunctionParameter);
		Assert.assertEquals("MyFunctionParameterValue1", myFunctionParameter);
		String myFunctionParameterComposed = (String) executionContext.getVariablesManager().getVariable("MyFunctionParameterComposed");
		Assert.assertNotNull(myFunctionParameterComposed);
		Assert.assertEquals("MyFunctionParameterValue1_MyFunctionParameterValue2", myFunctionParameterComposed);
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter2"));
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyAppParameter1"));

		executionContext = newExecutionContext(parameterManagerPlugin);
		parameterManagerPlugin.executionStart(executionContext);
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction("MyApp", "MyFunction2"));
		Object myFunctionParameter2 = executionContext.getVariablesManager().getVariable("MyFunctionParameter2");
		Assert.assertNotNull(myFunctionParameter2);
		Assert.assertEquals("MyFunctionParameterValue2", myFunctionParameter2);
		Object myAppParameter1 = executionContext.getVariablesManager().getVariable("MyAppParameter1");
		Assert.assertNotNull(myAppParameter1);
		Assert.assertEquals("MyOldGlobalParameterrValue1_MyGlobalParameterrValue1_MyAppParameterValue2", myAppParameter1);

		executionContext = newExecutionContext(parameterManagerPlugin);
		parameterManagerPlugin.executionStart(executionContext);
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction("MyApp", "MyFunction3"));
		Object myAppMyFunctionParameter3 = executionContext.getVariablesManager().getVariable("MyApp.MyFunctionParameter3");
		Assert.assertNotNull(myAppMyFunctionParameter3);
		Assert.assertEquals("MyApp.MyFunctionParameter3Value1", myAppMyFunctionParameter3);
		Object myFunctionParameter3 = executionContext.getVariablesManager().getVariable("MyFunctionParameter3");
		Assert.assertNotNull(myFunctionParameter3);
		Assert.assertEquals("MyFunctionParameter3Value1", myFunctionParameter3);
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter1"));
		Assert.assertNull(executionContext.getVariablesManager().getVariable("MyFunctionParameter2"));
	}

	@Test
	public void testDynamicValuesErrors() {
		// test backward compatibility where the scope is null
		declareParameter("MyOldGlobalParameter", "MyOldGlobalParameterrValue1", null, null);

		declareDynamicParameter("MyGlobalParameter", "\"${MyOldGlobalParameter}_MyGlobalParameterrValue1\"", ParameterScope.GLOBAL, null);

		declareDynamicParameter("MyAppParameter1", "\"${MyGlobalParameter}_MyAppParameterValue2\"", ParameterScope.APPLICATION, "MyApp");

		declareDynamicParameter("MyFunctionParameter", "\"MyFunctionParameterValue1\"", ParameterScope.FUNCTION, "MyFunction1");
		declareDynamicParameter("MyFunctionParameter2", "\"${MyFunctionParameterValue1}_MyFunctionParameterValue2\"", ParameterScope.FUNCTION, "MyFunction1");

		declareDynamicParameter("MyApp.MyFunctionParameter3", "\"MyApp.MyFunctionParameter3Value1\"", ParameterScope.FUNCTION, "MyApp.MyFunction3");
		declareDynamicParameter("MyFunctionParameter3", "\"MyFunctionParameter3Value1\"", ParameterScope.FUNCTION, "MyFunction3");


		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, new Configuration());
		final ExecutionContext executionContext = newExecutionContext(parameterManagerPlugin);

		Assert.assertThrows("Error while resolving parameters, following parameters could not be resolved: [MyFunctionParameter2]", PluginCriticalException.class, () -> parameterManagerPlugin.executionStart(executionContext));

	}


	private void assertVariable(VariablesManager variablesManager, String variableName) {
		String variable;
		variable = variablesManager.getVariableAsString(variableName);
		Assert.assertNotNull(variable);
		assertEquals("Value", variable);
	}

	private void assertProtectedVariable(VariablesManager variablesManager, String variableName) {
		Object variable = variablesManager.getVariable(variableName);
		Assert.assertNotNull(variable);
		Assert.assertTrue(variable instanceof ProtectedBinding);
		assertEquals("Value", ((ProtectedBinding) variable).value);
	}

	protected ExecutionContext newExecutionContext(ExecutionEnginePlugin parameterPlugin) {
		return ExecutionEngine.builder().withPlugin(parameterPlugin).build().newExecutionContext();
	}

	protected void declareParameter(String key, String value, ParameterScope scope, String scopeEntity) {
		Parameter functionParameter = newParameter(key, value, scope, scopeEntity);
		parameterAccessor.save(functionParameter);
	}

	private Parameter newParameter(String key, String value, ParameterScope scope, String scopeEntity) {
		Parameter functionParameter = new Parameter();
		functionParameter.setKey(key);
		functionParameter.setValue(new DynamicValue<>(value));
		functionParameter.setScope(scope);
		functionParameter.setScopeEntity(scopeEntity);
		return functionParameter;
	}
	
	protected void declareProtectedParameter(String key, String value, ParameterScope scope, String scopeEntity) {
		Parameter parameter = newParameter(key, value, scope, scopeEntity);
		parameter.setProtectedValue(true);
		parameterAccessor.save(parameter);
	}
	
	protected void declareProtectedAndEncryptedParameter(String key, String value, ParameterScope scope, String scopeEntity) {
		Parameter parameter = newParameter(key, value, scope, scopeEntity);
		parameter.setProtectedValue(true);
		parameter.setValue(null);
		try {
			parameter.setEncryptedValue(encryptionManager.encrypt(value));
		} catch (EncryptionManagerException e) {
			throw new RuntimeException(e);
		}
		parameterAccessor.save(parameter);
	}

	protected void declareDynamicParameter(String key, String value, ParameterScope scope, String scopeEntity) {
		Parameter functionParameter = newParameter(key, value, scope, scopeEntity);
		functionParameter.setValue(new DynamicValue<>(value, ""));
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
		public LocalParameterManagerPlugin(InMemoryAccessor<Parameter> parameterAccessor, Configuration configuration) {
			this(parameterAccessor, null, configuration);
		}

		public LocalParameterManagerPlugin(InMemoryAccessor<Parameter> parameterAccessor, EncryptionManager encryptionManager, Configuration configuration) {
			super(new ParameterManager(parameterAccessor, encryptionManager, configuration, new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler()))));
		}
		
		
	}
}
