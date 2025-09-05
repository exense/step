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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.InMemoryAccessor;
import step.core.accessors.LayeredAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicValue;
import step.core.encryption.EncryptionManagerException;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.core.execution.ExecutionEngineContext;
import step.core.objectenricher.ObjectPredicate;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.core.variables.VariableType;
import step.core.variables.VariablesManager;
import step.engine.execution.ExecutionManager;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.BasePlugin;
import step.expressions.ProtectedBinding;
import step.functions.Function;
import step.parameter.Parameter;
import step.parameter.ParameterManager;
import step.parameter.ParameterScope;
import step.security.SecurityManager;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Plugin(dependencies = {BasePlugin.class})
@IgnoreDuringAutoDiscovery
public class ParameterManagerPlugin extends AbstractExecutionEnginePlugin {

	public static String CONFIG_PROTECTED_PARAMETERS_ALWAYS_ALLOW_ACCESS = "plugins.parameters.protected.always.allow.access";
	
	private static final String RESOLVER_PREFIX_PARAMETER = "parameter:";
	private static final String PARAMETER_SCOPE_VALUE_DEFAULT = "default";
	private static final String PARAMETERS_BY_SCOPE = "$parametersByScope";

	public static Logger logger = LoggerFactory.getLogger(ParameterManagerPlugin.class);
		
	protected ParameterManager parameterManager;
	private boolean isConfigured = false;
	private boolean byPassProtectedParameters;

	protected ParameterManagerPlugin(){
	}

	public ParameterManagerPlugin(ParameterManager parameterManager) {
		super();
		configure(parameterManager);
	}

	protected void configure(ParameterManager parameterManager) {
		this.parameterManager = parameterManager;
		isConfigured = true;
	}

	@Override
	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext context) {
		if (!isConfigured) {
			return;
		}
		super.initializeExecutionContext(executionEngineContext, context);
		byPassProtectedParameters = context.getConfiguration().getPropertyAsBoolean(CONFIG_PROTECTED_PARAMETERS_ALWAYS_ALLOW_ACCESS, false);
		context.put(ParameterManager.class,
				ParameterManager.copy(parameterManager, new LayeredAccessor<>(List.of(new InMemoryAccessor<>(), parameterManager.getParameterAccessor())))
		);
	}

	@Override
	public void executionStart(ExecutionContext context) {
		if (!isConfigured) {
			return;
		}
		ReportNode rootNode = context.getReport();
		
		// Resolve the active parameters
		Map<String, Object> contextBindings = ExecutionContextBindings.get(context);
		ObjectPredicate objectPredicate = context.getObjectPredicate();
		Map<String, Parameter> allParameters = getParameterManagerFromContext(context).getAllParameters(contextBindings, objectPredicate);

		// Add all the active parameters to the execution parameter map of the Execution object
		buildExecutionParametersMapAndUpdateExecution(context, allParameters);
		
		initializeParameterResolver(context, allParameters);

		// Build the map of parameters by scope
		Map<ParameterScope, Map<String, List<Parameter>>> parametersByScope = getAllParametersByScope(allParameters);
		context.put(PARAMETERS_BY_SCOPE, parametersByScope);
		
		// Declare the global parameters including protected parameters if no encryption manager is available
		addScopeParametersToContext(context, rootNode, parametersByScope, ParameterScope.GLOBAL, PARAMETER_SCOPE_VALUE_DEFAULT);
	}
	
	@Override
	public void beforeFunctionExecution(ExecutionContext context, ReportNode node, Function function) {
		if (!isConfigured) {
			return;
		}
		// Function scoped parameters
		@SuppressWarnings("unchecked")
		Map<ParameterScope, Map<String, List<Parameter>>> parametersByScope = (Map<ParameterScope, Map<String, List<Parameter>>>) context.get(PARAMETERS_BY_SCOPE);
		
		Map<String, String> attributes = function.getAttributes();
		if(attributes != null) {
			addScopeParametersToContext(context, node, parametersByScope, ParameterScope.FUNCTION,
					attributes.get(AbstractOrganizableObject.NAME));
			if(attributes.containsKey(Function.APPLICATION)) {
				addScopeParametersToContext(context, node, parametersByScope, ParameterScope.FUNCTION,
						attributes.get(Function.APPLICATION) + "." + attributes.get(AbstractOrganizableObject.NAME));
				addScopeParametersToContext(context, node, parametersByScope, ParameterScope.APPLICATION,
						attributes.get(Function.APPLICATION));
			}
		}
	}

	protected ParameterManager getParameterManagerFromContext(ExecutionContext executionContext){
		return executionContext.require(ParameterManager.class);
	}

	private void buildExecutionParametersMapAndUpdateExecution(ExecutionContext context, Map<String, Parameter> allParameters) {
		ExecutionManager executionManager = context.getExecutionManager();
		// This map corresponds to the parameters displayed in the panel "Execution Parameters" of the execution view
		// which lists the parameters available in the plan after activation (evaluation of the activation expressions)
		Map<String, String> executionParameters = new HashMap<>();
		allParameters.forEach((k,v)->{
			DynamicValue<String> value = v.getValue();
			executionParameters.put(k, value != null ? value.get():"");
		});
		executionManager.updateExecution(execution-> {
			Map<String, String> parameters = execution.getParameters();
			if(parameters == null) {
				parameters = new HashMap<>();
				execution.setParameters(parameters);
			}
			parameters.putAll(executionParameters);
		});
	}

	private Map<ParameterScope, Map<String, List<Parameter>>> getAllParametersByScope(Map<String, Parameter> allParameters) {
		Map<ParameterScope, Map<String, List<Parameter>>> parametersByScope = new HashMap<>();
		allParameters.forEach((k,v)->{
			ParameterScope scope = v.getScope() != null ? v.getScope() : ParameterScope.GLOBAL;
			String scopeValue = v.getScopeEntity() != null ? v.getScopeEntity() : PARAMETER_SCOPE_VALUE_DEFAULT;
			parametersByScope.computeIfAbsent(scope, t->new HashMap<String, List<Parameter>>())
			.computeIfAbsent(scopeValue, t->new ArrayList<Parameter>())
			.add(v);
		});
		return parametersByScope;
	}

	private void addScopeParametersToContext(ExecutionContext context, ReportNode node, Map<ParameterScope, Map<String,
			List<Parameter>>> parametersByScope, ParameterScope scope, String scopeValue) {
		final VariablesManager varMan = context.getVariablesManager();
		Map<String, List<Parameter>> scopeSpecificParameters = parametersByScope.get(scope);
		if(scopeSpecificParameters != null) {
			List<Parameter> scopeValueSpecificParameters = scopeSpecificParameters.get(scopeValue);
			if(scopeValueSpecificParameters != null) {
				scopeValueSpecificParameters.forEach(p->{
					varMan.putVariable(node, VariableType.IMMUTABLE, p.getKey(), getParameterAsBindingValue(p, getParameterManagerFromContext(context), p.getKey()));
				});
			}
		}
	}

	//Used internally by the SQLTableDataPool to get the password, it's fine to have decrypted values here
	private void initializeParameterResolver(ExecutionContext context, Map<String, Parameter> parameters) {
		final ConcurrentMap<String, String> parameterValues = parameters.values().stream().map(p -> {
			return new SimpleEntry<String, String>(p.getKey(), getParameterValue(p, getParameterManagerFromContext(context)));
		}).collect(Collectors.toConcurrentMap(SimpleEntry::getKey, SimpleEntry::getValue));
		context.getResolver().register(e -> {
			if (e != null && e.startsWith(RESOLVER_PREFIX_PARAMETER)) {
				// As the parameterValues map may contain protected parameter values, calling
				// this method from custom scripts is forbidden
				SecurityManager.assertNotInExpressionHandler();
				String key = e.replace(RESOLVER_PREFIX_PARAMETER, "");
                return parameterValues.get(key);
			} else {
				return null;
			}
		});
	}

	private Object getParameterAsBindingValue(Parameter p, ParameterManager parameterManager, String key) {
		String value = getParameterValue(p, parameterManager);
		boolean isProtected = p.getProtectedValue();
		return byPassProtectedParameters ? value : (isProtected) ? new ProtectedBinding(value, key) : value;
	}

	private String getParameterValue(Parameter p, ParameterManager parameterManager) {
		String encryptedValue = p.getEncryptedValue();
		String value;
		if(encryptedValue != null) {
			if(parameterManager.getEncryptionManager() != null) {
				try {
					value = parameterManager.getEncryptionManager().decrypt(encryptedValue);
				} catch (EncryptionManagerException e) {
					throw new PluginCriticalException("Error while decrypting value of parameter "+p.getKey(), e);
				}
			} else {
				throw new PluginCriticalException("Unable to decrypt value of parameter "+p.getKey()+". No encryption manager available");
			}
		} else {
			value = p.getValue().get();
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
