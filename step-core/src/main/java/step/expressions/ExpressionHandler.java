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
package step.expressions;

import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.lang.Binding;
import groovy.lang.Script;

public class ExpressionHandler {
		
	private static Logger logger = LoggerFactory.getLogger(ExpressionHandler.class);
	
	private final GroovyPool groovyPool;
	
	private final Integer executionTimeWarningTreshold;
	
	public ExpressionHandler() {
		this(null);
	}
	
	public ExpressionHandler(String scriptBaseClass) {
		this(scriptBaseClass, null, 1000, -1);
	}
	
	public ExpressionHandler(String scriptBaseClass, Integer executionTimeWarningTreshold, int poolMaxTotal, int poolMaxIdle) {
		super();
		this.groovyPool = new GroovyPool(scriptBaseClass, poolMaxTotal, poolMaxIdle);
		this.executionTimeWarningTreshold = executionTimeWarningTreshold;
	}

	public Object evaluateGroovyExpression(String expression, Map<String, Object> bindings) {
		Object result;
		try {			
			logger.debug("Groovy evaluation:\n" + expression);

			Binding binding = new Binding(); 
			
			if(bindings!=null) {
				for(Entry<String, Object> varEntry : bindings.entrySet()) {
					Object value =  varEntry.getValue();
					binding.setVariable(varEntry.getKey(), value);
				}				
			}

			long t1 = System.currentTimeMillis();	
			try {
				GroovyPoolEntry entry = groovyPool.borrowShell(expression);
				try {
					Script script = entry.getScript();
					script.setBinding(binding);
					result = script.run();
				} finally {
					if(entry!=null && entry.getScript()!=null) {
						// Release bindings to avoid references to be kept by the pool
						entry.getScript().setBinding(new Binding());							
					}
					groovyPool.returnShell(entry);
				}
			} catch (Exception e) {
				logger.error("An error occurred while evaluation groovy expression " + expression, e);
				throw e;
			}
			long duration = System.currentTimeMillis()-t1;
			
			Integer warnThreshold = executionTimeWarningTreshold;
			if(warnThreshold!=null && duration > warnThreshold) {
				logger.warn("Groovy-Evaluation of following expression took " + duration + ".ms: "+ expression);
			} else {
				if(logger.isDebugEnabled()) {
					logger.debug("Groovy-Evaluation of following expression took " + duration + ".ms: "+ expression);
				}
			}
			
			if(logger.isDebugEnabled()) {
				logger.debug("Groovy result:\n" + result);
			}
			
			return result;
		} catch (CompilationFailedException cfe) {
			throw new RuntimeException(
					"Error in while compilating groovy expression: " + expression, cfe);
		} catch (Exception e){
			throw new RuntimeException(
					"Error in while running groovy expression: " + expression, e);
		}
	}

}
