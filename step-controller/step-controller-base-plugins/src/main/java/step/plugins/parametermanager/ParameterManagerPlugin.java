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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.parameter.Parameter;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNode;
import step.core.deployment.ObjectHookPlugin;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.core.objectenricher.ObjectPredicate;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.variables.VariableType;
import step.core.variables.VariablesManager;
import step.engine.execution.ExecutionManager;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.functions.Function;
import step.parameter.ParameterManager;
import step.parameter.ParameterScope;

@Plugin(dependencies = { ObjectHookPlugin.class })
@IgnoreDuringAutoDiscovery
public class ParameterManagerPlugin extends AbstractExecutionEnginePlugin {
	
	private static final String PARAMETER_SCOPE_VALUE_DEFAULT = "default";
	private static final String PARAMETERS_BY_SCOPE = "$parametersByScope";
	public static final String entityName = "parameters";

	public static Logger logger = LoggerFactory.getLogger(ParameterManagerPlugin.class);
		
	protected final ParameterManager parameterManager;
	
	public ParameterManagerPlugin(ParameterManager parameterManager) {
		super();
		this.parameterManager = parameterManager;
	}

	@Override
	public void executionStart(ExecutionContext context) {
		ReportNode rootNode = context.getReport();
		
		// Resolve the active parameters
		Map<String, Object> contextBindings = ExecutionContextBindings.get(context);
		ObjectPredicate objectPredicate = context.get(ObjectPredicate.class);
		Map<String, Parameter> allParameters = parameterManager.getAllParameters(contextBindings, objectPredicate);

		// Add all the active parameters to the execution parameter map of the Execution object
		buildExecutionParametersMapAndUpdateExecution(context, allParameters);
		
		// Build the map of parameters by scope
		Map<ParameterScope, Map<String, List<Parameter>>> parametersByScope = getAllParametersByScope(allParameters);
		context.put(PARAMETERS_BY_SCOPE, parametersByScope);
		
		// Declare the global parameters
		addScopeParametersToContext(context, rootNode, parametersByScope, ParameterScope.GLOBAL, PARAMETER_SCOPE_VALUE_DEFAULT);
	}
	
	@Override
	public void beforeFunctionExecution(ExecutionContext context, ReportNode node, Function function) {
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
			if(value != null) {
				executionParameters.put(k, value);
			}
		});
		executionManager.updateParameters(context, executionParameters);
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

	public static void putVariables(ExecutionContext context, ReportNode rootNode, Map<String, ? extends Object> parameters, VariableType type) {
		VariablesManager varMan = context.getVariablesManager();
		if(parameters!=null) {
			for(String key:parameters.keySet()) {
				varMan.putVariable(rootNode, type, key, parameters.get(key));
			}			
		}
	}
}
