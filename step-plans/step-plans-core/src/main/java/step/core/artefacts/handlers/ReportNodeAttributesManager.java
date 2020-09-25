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
package step.core.artefacts.handlers;

import java.util.HashMap;
import java.util.Map;

import step.core.execution.ExecutionContext;
import step.core.variables.VariablesManager;

public class ReportNodeAttributesManager {

	private ExecutionContext context;
	
	public ReportNodeAttributesManager(ExecutionContext context) {
		super();
		this.context = context;
	}

	private static String CUSTOM_ATTRIBUTES_PREFIX = "#customAttributes#";
	
	public void addCustomAttribute(String key, String value) {
		VariablesManager varMan = context.getVariablesManager();
		varMan.putVariable(context.getCurrentReportNode(), CUSTOM_ATTRIBUTES_PREFIX+key, value);
	}
	
	public Map<String, String> getCustomAttributes() {
		Map<String,String> result = new HashMap<>();
		VariablesManager varMan = context.getVariablesManager();
		Map<String, Object> allVars = varMan.getAllVariables();
		for(String varName:allVars.keySet()) {
			if(varName.startsWith(CUSTOM_ATTRIBUTES_PREFIX)) {
				String attributeKey = varName.substring(CUSTOM_ATTRIBUTES_PREFIX.length());
				result.put(attributeKey, (String) allVars.get(varName));
			}
		}
		return result;
	}
}
