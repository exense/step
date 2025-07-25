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
package step.commons.activation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator {

	public static final String DEFAULT_SCRIPT_ENGINE = "groovy";

	public static final Logger logger = LoggerFactory.getLogger(Activator.class);
	private static final ScriptEngineManager manager = new ScriptEngineManager();
	private static final ConcurrentHashMap<String, ScriptEngine> scriptEngines = new ConcurrentHashMap<>();

	public static void compileActivationExpression(ActivableObject object, String defaultScriptEngine) throws ScriptException {
		Expression expression = object.getActivationExpression();
		compileExpression(expression, defaultScriptEngine);
	}

	protected static void compileExpression(Expression expression, String defaultScriptEngine) throws ScriptException {
		if(expression!=null && expression.compiledScript==null) {
			if(expression.script!=null && expression.script.trim().length()>0) {
				ScriptEngine engine = getScriptEngineForExpression(expression, defaultScriptEngine);
				expression.compiledScript = ((Compilable)engine).compile(expression.script);
			} else {
				expression.compiledScript = null;
			}
		}
	}

	private static ScriptEngine getScriptEngineForExpression(Expression expression, String defaultScriptEngine) {
		String scriptEngine = expression.scriptEngine != null ? expression.scriptEngine : defaultScriptEngine;
		return scriptEngines.computeIfAbsent(scriptEngine, manager::getEngineByName);
	}

	public static Boolean evaluateActivationExpression(Bindings bindings, Expression activationExpression, String defaultScriptEngine) {
		Boolean expressionResult; 
		if(activationExpression!=null) {
			try {
				compileExpression(activationExpression, defaultScriptEngine);
			} catch (ScriptException e1) {
				logger.error("Error while evaluating expression "+activationExpression, e1);
			}
			// If the map wrapped by the bindings object is immutable (Map.of), it causes issues in the script engine
			// We therefore recreate a fresh bindings object
			Bindings newBindings = new SimpleBindings();
			if (bindings != null) {
				newBindings.putAll(bindings);
			}
			CompiledScript script = activationExpression.compiledScript;
			if(script!=null) {
				try {
					Object evaluationResult = script.eval(newBindings);
					if(evaluationResult instanceof Boolean) {
						expressionResult = (Boolean) evaluationResult;
					} else {
						expressionResult = false;
					}
				} catch (ScriptException e) {
					expressionResult = false;
				}
			} else {
				expressionResult = true;
			}
		} else {
			expressionResult = true;
		}
		return expressionResult;
	}
	
	public static <T extends ActivableObject> T findBestMatch(Map<String, Object> bindings, List<T> objects,String defaultScriptEngine) {
		return findBestMatch(bindings!=null?new SimpleBindings(bindings):null, objects, defaultScriptEngine);
	}
	
	private static <T extends ActivableObject> T findBestMatch(Bindings bindings, List<T> objects, String defaultScriptEngine) {
		
		List<T> matchingObjects = new ArrayList<>(objects);
		matchingObjects.sort(new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return -Integer.compare(getPriority(o1), getPriority(o2));
			}

			private int getPriority(T o1) {
				return o1.getActivationExpression()==null?0:(o1.getPriority()==null?1:o1.getPriority());
			}
		});
		
		for(T object:matchingObjects) {
			if(evaluateActivationExpression(bindings, object.getActivationExpression(), defaultScriptEngine)) {
				return object;
			}
		}
		return null;
	}
	
	public static <T extends ActivableObject> List<T> findAllMatches(Map<String, Object> bindings, List<T> objects, String defaultScriptEngine) {
		List<T> result = new ArrayList<>();
		for(T object:objects) {
			Boolean expressionResult = evaluateActivationExpression(bindings!=null?new SimpleBindings(bindings):null, object.getActivationExpression(), defaultScriptEngine);
			
			if(expressionResult) {
				result.add(object);
			}
		}
		return result;
	}
	
}

