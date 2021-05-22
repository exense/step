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

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNode;
import step.core.encryption.EncryptionManager;
import step.core.encryption.EncryptionManagerException;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.core.objectenricher.ObjectPredicate;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.core.variables.VariableType;
import step.core.variables.VariablesManager;
import step.engine.execution.ExecutionManager;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.functions.Function;
import step.parameter.Parameter;
import step.parameter.ParameterManager;
import step.parameter.ParameterScope;
import step.security.SecurityManager;

@Plugin(dependencies = {})
@IgnoreDuringAutoDiscovery
public class ParameterManagerPlugin extends AbstractExecutionEnginePlugin {
	
	private static final String RESOLVER_PREFIX_PARAMETER = "parameter:";
	private static final String PARAMETER_SCOPE_VALUE_DEFAULT = "default";
	private static final String PARAMETERS_BY_SCOPE = "$parametersByScope";
	private static final String PROTECTED_PARAMETERS = "$parametersProtected";

	public static Logger logger = LoggerFactory.getLogger(ParameterManagerPlugin.class);
		
	protected final ParameterManager parameterManager;
	protected final EncryptionManager encryptionManager;
	
	public ParameterManagerPlugin(ParameterManager parameterManager, EncryptionManager encryptionManager) {
		super();
		this.parameterManager = parameterManager;
		this.encryptionManager = encryptionManager;
	}

	@Override
	public void executionStart(ExecutionContext context) {
		ReportNode rootNode = context.getReport();
		
		// Resolve the active parameters
		Map<String, Object> contextBindings = ExecutionContextBindings.get(context);
		ObjectPredicate objectPredicate = context.getObjectPredicate();
		Map<String, Parameter> allParameters = parameterManager.getAllParameters(contextBindings, objectPredicate);

		// Add all the active parameters to the execution parameter map of the Execution object
		buildExecutionParametersMapAndUpdateExecution(context, allParameters);
		
		initializeParameterResolver(context, allParameters);
		
		// Build the map of parameters by scope
		Map<ParameterScope, Map<String, List<Parameter>>> parametersByScope = getAllParametersByScope(allParameters);
		context.put(PARAMETERS_BY_SCOPE, parametersByScope);
		
		List<Parameter> protectedParameters = allParameters.values().stream()
				.filter(p -> p.getProtectedValue() != null && p.getProtectedValue()).collect(Collectors.toList());
		context.put(PROTECTED_PARAMETERS, protectedParameters);
		
		// Declare the global parameters
		addScopeParametersToContext(context, rootNode, parametersByScope, ParameterScope.GLOBAL, PARAMETER_SCOPE_VALUE_DEFAULT);
	}
	
	@Override
	public void beforeFunctionExecution(ExecutionContext context, ReportNode node, Function function) {
		// Protected parameters
		@SuppressWarnings("unchecked")
		List<Parameter> protectedParameters = (List<Parameter>) context.get(PROTECTED_PARAMETERS);
		addProtectedParametersToContext(context, node, protectedParameters);

		// Function scoped parameters
		@SuppressWarnings("unchecked")
		Map<ParameterScope, Map<String, List<Parameter>>> parametersByScope = (Map<ParameterScope, Map<String, List<Parameter>>>) context.get(PARAMETERS_BY_SCOPE);
		
		Map<String, String> attributes = function.getAttributes();
		if(attributes != null) {
			addScopeParametersToContext(context, node, parametersByScope, ParameterScope.FUNCTION, attributes.get(AbstractOrganizableObject.NAME));
			if(attributes.containsKey(Function.APPLICATION)) {
				addScopeParametersToContext(context, node, parametersByScope, ParameterScope.FUNCTION, attributes.get(Function.APPLICATION)+"."+attributes.get(AbstractOrganizableObject.NAME));
				addScopeParametersToContext(context, node, parametersByScope, ParameterScope.APPLICATION, attributes.get(Function.APPLICATION));
			}
		}
	}

	private void buildExecutionParametersMapAndUpdateExecution(ExecutionContext context, Map<String, Parameter> allParameters) {
		ExecutionManager executionManager = context.getExecutionManager();
		// This map corresponds to the parameters displayed in the panel "Execution Parameters" of the execution view
		// which lists the parameters available in the plan after activation (evaluation of the activation expressions)
		Map<String, String> executionParameters = new HashMap<>();
		allParameters.forEach((k,v)->{
			String value = v.getValue();
			executionParameters.put(k, value != null ? value:"");
		});
		executionManager.updateParameters(context, executionParameters);
	}

	private Map<ParameterScope, Map<String, List<Parameter>>> getAllParametersByScope(Map<String, Parameter> allParameters) {
		Map<ParameterScope, Map<String, List<Parameter>>> parametersByScope = new HashMap<>();
		allParameters.forEach((k,v)->{
			Boolean isProtectedValue = v.getProtectedValue();
			if(isProtectedValue == null || !isProtectedValue) {
				ParameterScope scope = v.getScope() != null ? v.getScope() : ParameterScope.GLOBAL;
				String scopeValue = v.getScopeEntity() != null ? v.getScopeEntity() : PARAMETER_SCOPE_VALUE_DEFAULT;
				parametersByScope.computeIfAbsent(scope, t->new HashMap<String, List<Parameter>>())
				.computeIfAbsent(scopeValue, t->new ArrayList<Parameter>())
				.add(v);
			}
		});
		return parametersByScope;
	}

	private void addScopeParametersToContext(ExecutionContext context, ReportNode node, Map<ParameterScope, Map<String, List<Parameter>>> parametersByScope, 
			ParameterScope scope, String scopeValue) {
		final VariablesManager varMan = context.getVariablesManager();
		Map<String, List<Parameter>> scopeSpecificParameters = parametersByScope.get(scope);
		if(scopeSpecificParameters != null) {
			List<Parameter> scopeValueSpecificParameters = scopeSpecificParameters.get(scopeValue);
			if(scopeValueSpecificParameters != null) {
				scopeValueSpecificParameters.forEach(p->{
					varMan.putVariable(node, VariableType.IMMUTABLE, p.getKey(), p.getValue());
				});
			}
		}
	}
	
	private void initializeParameterResolver(ExecutionContext context, Map<String, Parameter> parameters) {
		final ConcurrentMap<String, String> parameterValues = parameters.values().stream().map(p -> {
			return new SimpleEntry<String, String>(p.getKey(), getParameterValue(p));
		}).collect(Collectors.toConcurrentMap(SimpleEntry::getKey, SimpleEntry::getValue));
		context.getResolver().register(e -> {
			if (e != null && e.startsWith(RESOLVER_PREFIX_PARAMETER)) {
				// As the parameterValues map may contain protected parameter values, calling
				// this method from custom scripts is forbidden
				SecurityManager.assertNotInExpressionHandler();
				String key = e.replace(RESOLVER_PREFIX_PARAMETER, "");
				String value = parameterValues.get(key);
				if (value != null) {
					return value;
				} else {
					return null;
				}
			} else {
				return null;
			}
		});
	}
	
	private void addProtectedParametersToContext(ExecutionContext context, ReportNode node, List<Parameter> protectedParameters) {
		final VariablesManager varMan = context.getVariablesManager();
		protectedParameters.forEach(p -> {
			String value = getParameterValue(p);
			varMan.putVariable(node, VariableType.IMMUTABLE, p.getKey(), value);
		});
	}

	public String getParameterValue(Parameter p) {
		String encryptedValue = p.getEncryptedValue();
		String value;
		if(encryptedValue != null) {
			if(encryptionManager != null) {
				try {
					value = encryptionManager.decrypt(encryptedValue);
				} catch (EncryptionManagerException e) {
					throw new PluginCriticalException("Error while decrypting value of parameter "+p.getKey(), e);
				}
			} else {
				throw new PluginCriticalException("Unable to decrypt value of parameter "+p.getKey()+". No encryption manager available");
			}
		} else {
			value = p.getValue();
		}
		return value;
	}

	public static void putVariables(ExecutionContext context, ReportNode rootNode, Map<String, ? extends Object> parameters, VariableType type) {
		VariablesManager varMan = context.getVariablesManager();
		if(parameters!=null) {
			for(String key:parameters.keySet()) {
				varMan.putVariable(rootNode, type, key, parameters.get(key));
			}			
		}
	}
}
