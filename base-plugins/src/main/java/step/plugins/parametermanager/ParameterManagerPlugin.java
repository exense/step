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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.accessors.AbstractCRUDAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.core.variables.VariableType;
import step.core.variables.VariablesManager;

@Plugin(prio=10)
public class ParameterManagerPlugin extends AbstractPlugin {
	
	public static Logger logger = LoggerFactory.getLogger(ParameterManagerPlugin.class);
		
	@Override
	public void executionControllerStart(GlobalContext context) {
		AbstractCRUDAccessor<Parameter> parameterAccessor = new AbstractCRUDAccessor<>(context.getMongoClientSession(), "parameters", Parameter.class);
		context.put("ParameterAccessor", parameterAccessor);
		
		ParameterManager parameterManager = new ParameterManager(parameterAccessor);
		context.put(ParameterManager.class, parameterManager);
		
		context.getServiceRegistrationCallback().registerService(ParameterServices.class);
	}

	@Override
	public void executionStart(ExecutionContext context) {
		ParameterManager parameterManager = context.getGlobalContext().get(ParameterManager.class);
		
		if(parameterManager!=null) {
			ReportNode rootNode = context.getReport();
			VariablesManager varMan = context.getVariablesManager();
			varMan.putVariable(rootNode, VariableType.IMMUTABLE, "user", context.getExecutionParameters().getUserID());
			putVariables(context, rootNode, context.getExecutionParameters().getCustomParameters(), VariableType.IMMUTABLE);
			
			Map<String, String> parameters = parameterManager.getAllParameters(ExecutionContextBindings.get(context));
			putVariables(context, rootNode, parameters, VariableType.IMMUTABLE);		
		} else {
			logger.warn("Not able to read parameters. ParameterManager has not been initialized during controller start.");
		}
		super.executionStart(context);
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
