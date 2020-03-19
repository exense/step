/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.plugins.parametermanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.collections.CollectionRegistry;
import step.core.artefacts.reports.ReportNode;
import step.core.deployment.ObjectHookPlugin;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.core.execution.ExecutionManager;
import step.core.execution.model.ExecutionParameters;
import step.core.objectenricher.ObjectPredicate;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.variables.VariableType;
import step.core.variables.VariablesManager;
import step.functions.Function;
import step.plugins.screentemplating.Input;
import step.plugins.screentemplating.InputType;
import step.plugins.screentemplating.ScreenInput;
import step.plugins.screentemplating.ScreenInputAccessor;
import step.plugins.screentemplating.ScreenTemplatePlugin;

@Plugin(dependencies= {ObjectHookPlugin.class, ScreenTemplatePlugin.class})
public class ParameterManagerPlugin extends AbstractControllerPlugin {
	
	private static final String PARAMETER_SCOPE_VALUE_DEFAULT = "default";
	private static final String PARAMETERS_BY_SCOPE = "$parametersByScope";

	public static Logger logger = LoggerFactory.getLogger(ParameterManagerPlugin.class);
		
	protected ParameterManager parameterManager;
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		AbstractCRUDAccessor<Parameter> parameterAccessor = new AbstractCRUDAccessor<>(context.getMongoClientSession(), "parameters", Parameter.class);
		context.put("ParameterAccessor", parameterAccessor);
		
		context.get(CollectionRegistry.class).register("parameters", new ParameterCollection(context.getMongoClientSession().getMongoDatabase()));
		
		ParameterManager parameterManager = new ParameterManager(parameterAccessor);
		context.put(ParameterManager.class, parameterManager);
		this.parameterManager = parameterManager;
		
		context.getServiceRegistrationCallback().registerService(ParameterServices.class);
	}

	@Override
	public void initializeData(GlobalContext context) throws Exception {
		createScreenInputDefinitionsIfNecessary(context);
	}

	private static final String PARAMETER_DIALOG = "parameterDialog";
	private static final String PARAMETER_TABLE = "parameterTable";

	protected void createScreenInputDefinitionsIfNecessary(GlobalContext context) {
		// Parameter table
		ScreenInputAccessor screenInputAccessor = context.get(ScreenInputAccessor.class);
		List<ScreenInput> parameterTable = screenInputAccessor.getScreenInputsByScreenId(PARAMETER_TABLE);
		Input keyInput = new Input(InputType.TEXT, "key", "Key", "Keys containing 'pwd' or 'password' will be automatically protected", null);
		keyInput.setValueHtmlTemplate("<entity-icon entity=\"stBean\" entity-name=\"'parameter'\"/> <parameter-key parameter=\"stBean\" />");
		if(parameterTable.isEmpty()) {
			screenInputAccessor.save(new ScreenInput(0, PARAMETER_TABLE, keyInput));
			screenInputAccessor.save(new ScreenInput(1, PARAMETER_TABLE, new Input(InputType.TEXT, "value", "Value", null, null)));
			screenInputAccessor.save(new ScreenInput(2, PARAMETER_TABLE, new Input(InputType.TEXT, "activationExpression.script", "Activation script", null, null)));
			screenInputAccessor.save(new ScreenInput(3, PARAMETER_TABLE, new Input(InputType.TEXT, "priority", "	Priority", null, null)));
		}
		
		// Ensure the key input is always up to date
		parameterTable.forEach(i->{
			Input input = i.getInput();
			if(input.getId().equals("key")) {
				i.setInput(keyInput);
				screenInputAccessor.save(i);
			}
		});
		
		// Edit parameter dialog
		if(screenInputAccessor.getScreenInputsByScreenId(PARAMETER_DIALOG).isEmpty()) {
			Input input = new Input(InputType.TEXT, "key", "Key", "Keys containing 'pwd' or 'password' will be automatically protected", null);
			screenInputAccessor.save(new ScreenInput(0, PARAMETER_DIALOG, input));
			screenInputAccessor.save(new ScreenInput(1, PARAMETER_DIALOG, new Input(InputType.TEXT, "value", "Value", null, null)));
			screenInputAccessor.save(new ScreenInput(2, PARAMETER_DIALOG, new Input(InputType.TEXT, "description", "Description", null, null)));
			screenInputAccessor.save(new ScreenInput(3, PARAMETER_DIALOG, new Input(InputType.TEXT, "activationExpression.script", "Activation script", null, null)));
			screenInputAccessor.save(new ScreenInput(4, PARAMETER_DIALOG, new Input(InputType.TEXT, "priority", "	Priority", null, null)));
		}
	}

	@Override
	public void executionStart(ExecutionContext context) {
		
		if(parameterManager!=null) {
			ReportNode rootNode = context.getReport();
			
			// Create the contextual global parameters 
			Map<String, String> globalParametersFromExecutionParameters = new HashMap<>();
			ExecutionParameters executionParameters = context.getExecutionParameters();
			if(executionParameters.getUserID() != null) {
				globalParametersFromExecutionParameters.put("user", executionParameters.getUserID());
			}
			if(executionParameters.getCustomParameters() != null) {
				globalParametersFromExecutionParameters.putAll(executionParameters.getCustomParameters());			
			}
			putVariables(context, rootNode, globalParametersFromExecutionParameters, VariableType.IMMUTABLE);
			
			// Resolve the active parameters
			Map<String, Object> contextBindings = ExecutionContextBindings.get(context);
			ObjectPredicate objectPredicate = context.get(ObjectPredicate.class);
			Map<String, Parameter> allParameters = parameterManager.getAllParameters(contextBindings, objectPredicate);

			// Add all the active parameters to the execution parameter map of the Execution object
			buildExecutionParametersMapAndUpdateExecution(context, globalParametersFromExecutionParameters, allParameters);
			
			// Build the map of parameters by scope
			Map<ParameterScope, Map<String, List<Parameter>>> parametersByScope = getAllParametersByScope(allParameters);
			context.put(PARAMETERS_BY_SCOPE, parametersByScope);
			
			// Declare the global parameters
			addScopeParametersToContext(context, rootNode, parametersByScope, ParameterScope.GLOBAL, PARAMETER_SCOPE_VALUE_DEFAULT);
		} else {
			logger.warn("Not able to read parameters. ParameterManager has not been initialized during controller start.");
		}
		super.executionStart(context);
	}
	
	@Override
	public void beforeFunctionExecution(ExecutionContext context, ReportNode node, Function function) {
		super.beforeFunctionExecution(context, node, function);
		
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

	protected void buildExecutionParametersMapAndUpdateExecution(ExecutionContext context,
			Map<String, String> additionalGlobalParameters, Map<String, Parameter> allParameters) {
		ExecutionManager executionManager = context.get(ExecutionManager.class);
		if(executionManager != null) {
			// This map corresponds to the parameters displayed in the panel "Execution Parameters" of the execution view
			// which lists the parameters available in the plan after activation (evaluation of the activation expressions)
			Map<String, String> executionParameters = new HashMap<>();
			executionParameters.putAll(additionalGlobalParameters);
			executionParameters.putAll(allParameters.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e->e.getValue().getValue())));
			executionManager.updateParameters(context, executionParameters);
		}
	}

	protected Map<ParameterScope, Map<String, List<Parameter>>> getAllParametersByScope(Map<String, Parameter> allParameters) {
		Map<ParameterScope, Map<String, List<Parameter>>> parametersByScope = new HashMap<>();
		allParameters.forEach((k,v)->{
			ParameterScope scope = v.scope != null ? v.scope : ParameterScope.GLOBAL;
			String scopeValue = v.scopeEntity != null ? v.scopeEntity : PARAMETER_SCOPE_VALUE_DEFAULT;
			parametersByScope.computeIfAbsent(scope, t->new HashMap<String, List<Parameter>>())
							 .computeIfAbsent(scopeValue, t->new ArrayList<Parameter>())
							 .add(v);
		});
		return parametersByScope;
	}

	protected void addScopeParametersToContext(ExecutionContext context, ReportNode node, Map<ParameterScope, Map<String, List<Parameter>>> parametersByScope, 
			ParameterScope scope, String scopeValue) {
		final VariablesManager varMan = context.getVariablesManager();
		Map<String, List<Parameter>> scopeSpecificParameters = parametersByScope.get(scope);
		if(scopeSpecificParameters != null) {
			List<Parameter> scopeValueSpecificParameters = scopeSpecificParameters.get(scopeValue);
			if(scopeValueSpecificParameters != null) {
				scopeValueSpecificParameters.forEach(p->{
					varMan.putVariable(node, VariableType.IMMUTABLE, p.key, p.value);
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
