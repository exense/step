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

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import step.commons.activation.Expression;
import step.commons.conf.Configuration;
import step.commons.conf.FileRepository;
import step.commons.conf.FileRepository.FileRepositoryCallback;
import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.core.variables.VariableType;
import step.core.variables.VariablesManager;

@Plugin
public class ParameterManagerPlugin extends AbstractPlugin {
	
	public static Logger logger = LoggerFactory.getLogger(ParameterManagerPlugin.class);
	
	public static final String KEY = "ParameterManager_Instance";
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		String config = Configuration.getInstance().getProperty("parametermanager.config");
		if(config!=null) {
			new FileRepository<>(new File(config), JsonNode.class, new FileRepositoryCallback<JsonNode>() {
				@Override
				public void onLoad(JsonNode object) throws Exception {
					ParameterManager parameterManager = new ParameterManager();
					for(JsonNode o:object) {
						Expression expression = null;
						String expressionStr = o.get(2).asText();
						if(expressionStr!=null&&expressionStr.length()>0) {
							expression = new Expression(expressionStr);
						}
						Parameter p = new Parameter(expression, o.get(0).asText(), o.get(1).asText());
						if(o.has(3)) {
							p.setPriority(o.get(3).asInt());
						}
						parameterManager.addParameter(p);
					}
					context.put(KEY, parameterManager);
				}
			});
		}
	}

	@Override
	public void executionStart(ExecutionContext context) {
		ParameterManager parameterManager = (ParameterManager) context.getGlobalContext().get(ParameterManagerPlugin.KEY);
		
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
		
	private void putVariables(ExecutionContext context, ReportNode rootNode, Map<String, ? extends Object> parameters, VariableType type) {
		VariablesManager varMan = context.getVariablesManager();
		for(String key:parameters.keySet()) {
			varMan.putVariable(rootNode, type, key, parameters.get(key));
		}
	}

}
