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

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.commons.activation.Activator;
import step.core.accessors.CRUDAccessor;
import step.core.objectenricher.ObjectPredicate;

public class ParameterManager {
	
	private static Logger logger = LoggerFactory.getLogger(ParameterManager.class);
	
	private CRUDAccessor<Parameter> parameterAccessor;

	private String defaultScriptEngine;
	
	public ParameterManager(CRUDAccessor<Parameter> parameterAccessor, Configuration configuration) {
		super();
		this.parameterAccessor = parameterAccessor;
		this.defaultScriptEngine = configuration.getProperty("tec.activator.scriptEngine", Activator.DEFAULT_SCRIPT_ENGINE);
	}

	public Map<String, String> getAllParameterValues(Map<String, Object> contextBindings, ObjectPredicate objectPredicate) {
		return getAllParameters(contextBindings, objectPredicate).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().value));
	}
	
	public Map<String, Parameter> getAllParameters(Map<String, Object> contextBindings, ObjectPredicate objectPredicate) {
		Map<String, Parameter> result = new HashMap<>();
		Bindings bindings = contextBindings!=null?new SimpleBindings(contextBindings):null;

		Map<String, List<Parameter>> parameterMap = new HashMap<String, List<Parameter>>();
		parameterAccessor.getAll().forEachRemaining(p->{
			if(objectPredicate==null || objectPredicate.test(p)) {
				List<Parameter> parameters = parameterMap.get(p.key);
				if(parameters==null) {
					parameters = new ArrayList<>();
					parameterMap.put(p.key, parameters);
				}
				parameters.add(p);
				try {
					Activator.compileActivationExpression(p, defaultScriptEngine);
				} catch (ScriptException e) {
					logger.error("Error while compilating activation expression of parameter "+p, e);
				}
			}
		});
		
		
		for(String key:parameterMap.keySet()) {
			List<Parameter> parameters = parameterMap.get(key);
			Parameter bestMatch = Activator.findBestMatch(bindings, parameters, defaultScriptEngine);
			if(bestMatch!=null) {
				result.put(key, bestMatch);
			}
		}
		return result;
	}

	public String getDefaultScriptEngine() {
		return defaultScriptEngine;
	}

}
