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

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import step.commons.activation.Activator;

public class ParameterManager {
	
	Map<String, List<Parameter>> parameterMap = new HashMap<String, List<Parameter>>();
	
	public void clearParameters() {
		parameterMap.clear();
	}
	
	public void addParameters(List<Parameter> parameters) throws ScriptException {
		for(Parameter parameter:parameters) {
			addParameter(parameter);
		}
	}
	
	public void addParameter(Parameter parameter) throws ScriptException {
		Activator.compileActivationExpression(parameter);
		
		List<Parameter> parameters = parameterMap.get(parameter.key);
		if(parameters==null) {
			parameters = new ArrayList<>();
			parameterMap.put(parameter.key, parameters);
		}
		
		parameters.add(parameter);
	}
	
	public Map<String, String> getAllParameters(Map<String, Object> contextBindings) {
		Map<String, String> result = new HashMap<>();
		
		Bindings bindings = contextBindings!=null?new SimpleBindings(contextBindings):null;
		
		for(String key:parameterMap.keySet()) {
			List<Parameter> parameters = parameterMap.get(key);
			Parameter bestMatch = Activator.findBestMatch(bindings, parameters);
			if(bestMatch!=null) {
				result.put(key, bestMatch.value);
			}
		}
		return result;
	}
}
