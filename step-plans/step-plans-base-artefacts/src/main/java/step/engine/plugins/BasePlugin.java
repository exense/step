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
package step.engine.plugins;

import java.util.HashMap;
import java.util.Map;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ExecutionParameters;
import step.core.plugins.Plugin;
import step.core.variables.VariableType;
import step.core.variables.VariablesManager;

@Plugin(dependencies= {})
public class BasePlugin extends AbstractExecutionEnginePlugin {

	@Override
	public void executionStart(ExecutionContext context) {
		super.executionStart(context);
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
		VariablesManager variablesManager = context.getVariablesManager();
		globalParametersFromExecutionParameters.forEach((k,v)->variablesManager.putVariable(rootNode, VariableType.IMMUTABLE, k, v));
	}
}
