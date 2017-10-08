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
package step.commons.activation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

public class Activator {

	private static final String DEFAULT_SCRIPT_ENGINE = "nashorn";

	public static <T extends ActivableObject> List<T> compileActivationExpressions(List<T> objects) throws ScriptException {
		for(ActivableObject object:objects) {
			compileActivationExpression(object);
		}
		return objects; 
	}
	
	public static void compileActivationExpression(ActivableObject object) throws ScriptException {
		Expression expression = object.getActivationExpression();
		if(expression!=null && expression.compiledScript==null) {
			String scriptEngine = expression.scriptEngine!=null?expression.scriptEngine:DEFAULT_SCRIPT_ENGINE;

			if(expression.script!=null && expression.script.trim().length()>0) {
				ScriptEngineManager manager = new ScriptEngineManager();
		        ScriptEngine engine = manager.getEngineByName(scriptEngine);

		        CompiledScript script = ((Compilable)engine).compile(expression.script);
		        expression.compiledScript = script;
			} else {
				expression.compiledScript = null;
			}
		}
	}
	
	public static Boolean evaluateActivationExpression(Bindings bindings, Expression activationExpression) {
		Boolean expressionResult; 
		if(activationExpression!=null) {
			CompiledScript script = activationExpression.compiledScript;
			if(script!=null) {
				try {
					Object evaluationResult = script.eval(bindings);
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
	
	public static <T extends ActivableObject> T findBestMatch(Map<String, Object> bindings, List<T> objects) {
		return findBestMatch(bindings!=null?new SimpleBindings(bindings):null, objects);
	}
	
	private static <T extends ActivableObject> T findBestMatch(Bindings bindings, List<T> objects) {
		
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
			if(evaluateActivationExpression(bindings, object.getActivationExpression())) {
				return object;
			}
		}
		return null;
	}
	
	public static <T extends ActivableObject> List<T> findAllMatches(Map<String, Object> bindings, List<T> objects) {
		List<T> result = new ArrayList<>();
		for(T object:objects) {
			Boolean expressionResult = evaluateActivationExpression(bindings!=null?new SimpleBindings(bindings):null, object.getActivationExpression());
			
			if(expressionResult) {
				result.add(object);
			}
		}
		return result;
	}
	
}

