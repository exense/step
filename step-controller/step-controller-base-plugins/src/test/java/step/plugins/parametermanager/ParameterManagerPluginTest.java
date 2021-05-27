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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.exense.commons.core.model.accessors.AbstractOrganizableObject;
import ch.exense.commons.core.accessors.InMemoryAccessor;
import step.core.encryption.EncryptionManager;
import step.core.encryption.EncryptionManagerException;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.variables.VariablesManager;
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
	
	@Test
	public void testProtectedParameters() {
		declareProtectedParameter("MyProtectedParameterWithoutScope", "Value", null, null);
		declareProtectedParameter("MyProtectedParameterWithGlobalScope", "Value", ParameterScope.GLOBAL, null);
		declareProtectedParameter("MyProtectedParameterWithOtherApplicationScope", "Value", ParameterScope.APPLICATION, "MyOtherApp");
		declareProtectedParameter("MyProtectedParameterWithApplicationScope", "Value", ParameterScope.APPLICATION, "MyApp");
		declareProtectedParameter("MyProtectedParameterWithFunctionScope", "Value", ParameterScope.FUNCTION, "MyFunction1");
		
		ExecutionContext executionContext = newExecutionContext();
		VariablesManager variablesManager = executionContext.getVariablesManager();

		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, encryptionManager, executionContext);
		parameterManagerPlugin.executionStart(executionContext);
		// None of the parameters should be available as they are all protected
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithoutScope"));
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithGlobalScope"));
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithApplicationScope"));
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithFunctionScope"));
		
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction("MyApp", "MyFunction1"));
		
		// All parameters matching the scope should now be available
		Assert.assertNotNull(variablesManager.getVariable("MyProtectedParameterWithoutScope"));
		Assert.assertNotNull(variablesManager.getVariable("MyProtectedParameterWithGlobalScope"));
		Assert.assertNotNull(variablesManager.getVariable("MyProtectedParameterWithApplicationScope"));
		Assert.assertNotNull(variablesManager.getVariable("MyProtectedParameterWithFunctionScope"));

		// Parameters not matching the scope should 
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithOtherApplicationScope"));
	}
	
	@Test
	public void testProtectedParametersWithoutEncryptionManager() {
		declareProtectedParameter("MyProtectedParameterWithoutScope", "Value", null, null);
		declareProtectedParameter("MyProtectedParameterWithGlobalScope", "Value", ParameterScope.GLOBAL, null);
		declareProtectedParameter("MyProtectedParameterWithApplicationScope", "Value", ParameterScope.APPLICATION, "MyApp");
		declareProtectedParameter("MyProtectedParameterWithFunctionScope", "Value", ParameterScope.FUNCTION, "MyFunction1");
		
		ExecutionContext executionContext = newExecutionContext();
		VariablesManager variablesManager = executionContext.getVariablesManager();

		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, executionContext);
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
		
		ExecutionContext executionContext = newExecutionContext();
		VariablesManager variablesManager = executionContext.getVariablesManager();

		// Construct a ParameterManagerPlugin with encryption manager
		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, encryptionManager, executionContext);
		parameterManagerPlugin.executionStart(executionContext);
		// None of the parameters should be available as they are all protected
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithoutScope"));
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithGlobalScope"));
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithApplicationScope"));
		Assert.assertNull(variablesManager.getVariable("MyProtectedParameterWithFunctionScope"));
		
		parameterManagerPlugin.beforeFunctionExecution(executionContext, executionContext.getCurrentReportNode(), newFunction("MyApp", "MyFunction1"));

		// All parameters should now be available and their value should be decrypted
		assertVariable(variablesManager, "MyProtectedParameterWithoutScope");
		assertVariable(variablesManager, "MyProtectedParameterWithGlobalScope");
		assertVariable(variablesManager, "MyProtectedParameterWithApplicationScope");
		assertVariable(variablesManager, "MyProtectedParameterWithFunctionScope");
	}
	
	@Test
	public void testEncryptedParametersAndNoEncryptionManagerAvailable() {
		declareProtectedAndEncryptedParameter("MyProtectedParameterWithoutScope", "Value", null, null);

		ExecutionContext executionContext = newExecutionContext();

		// Construct a ParameterManagerPlugin without encryption manager
		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, null, executionContext);

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

		ExecutionContext executionContext = newExecutionContext();

		// Construct ParameterManagerPlugin with error encryption manager (failing on decrypt())
		ParameterManagerPlugin parameterManagerPlugin = new LocalParameterManagerPlugin(parameterAccessor, errorEncryptionManager, executionContext);

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

	private void assertVariable(VariablesManager variablesManager, String variableName) {
		String variable;
		variable = variablesManager.getVariableAsString(variableName);
		Assert.assertNotNull(variable);
		assertEquals("Value", variable);
	}

	protected ExecutionContext newExecutionContext() {
		return ExecutionEngine.builder().build().newExecutionContext();
	}

	protected void declareParameter(String key, String value, ParameterScope scope, String scopeEntity) {
		Parameter functionParameter = newParameter(key, value, scope, scopeEntity);
		parameterAccessor.save(functionParameter);
	}

	private Parameter newParameter(String key, String value, ParameterScope scope, String scopeEntity) {
		Parameter functionParameter = new Parameter();
		functionParameter.setKey(key);
		functionParameter.setValue(value);
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

		public LocalParameterManagerPlugin(InMemoryAccessor<Parameter> parameterAccessor, ExecutionContext executionContext) {
			this(parameterAccessor, null, executionContext);
		}

		public LocalParameterManagerPlugin(InMemoryAccessor<Parameter> parameterAccessor, EncryptionManager encryptionManager, ExecutionContext executionContext) {
			super(new ParameterManager(parameterAccessor, encryptionManager, executionContext.getConfiguration()), encryptionManager);
		}
		
		
	}
}
