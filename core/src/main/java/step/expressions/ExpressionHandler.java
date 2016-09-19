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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import step.commons.conf.Configuration;

public class ExpressionHandler {
		
	private static Logger logger = LoggerFactory.getLogger(ExpressionHandler.class);
	
	private static final GroovyClassLoader groovyLoader = new GroovyClassLoader();
		
	private final CompilerConfiguration groovyCompilerConfiguration;
	
	public ExpressionHandler() {
		this("step.expressions.GroovyFunctions");
	}
	
	public ExpressionHandler(String baseScriptBase ) {
		super();
		
		groovyCompilerConfiguration = new CompilerConfiguration();
		if(baseScriptBase!=null) {
			groovyCompilerConfiguration.setScriptBaseClass(baseScriptBase);
		}
	}
	
	static Pattern expressionPattern = Pattern.compile("\\[\\[(.+?)\\]\\]");
	
	public String evaluate(String original, Map<String, Object> bindings) {
		StringBuffer sb = new StringBuffer();
		Matcher m = expressionPattern.matcher(original);
		while(m.find()) {
			String expression = m.group(1);
			Object result = evaluateGroovyExpression(expression, bindings);
			if(result!=null) {
				m.appendReplacement(sb, result.toString());				
			} else {
				m.appendReplacement(sb, "");
			}
		}
		m.appendTail(sb);
		return sb.toString();
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
				if(Configuration.getInstance().getPropertyAsBoolean("tec.expressions.usecache",true)) {
					GroovyPoolEntry entry = GroovyPool.getINSTANCE().borrowShell(expression);
					try {
						Script script = entry.getScript();
						script.setBinding(binding);
						result = script.run();
					} finally {
						if(entry!=null && entry.getScript()!=null) {
							// Release bindings to avoid references to be kept by the pool
							entry.getScript().setBinding(new Binding());							
						}
						GroovyPool.getINSTANCE().returnShell(entry);
					}
				} else {
					GroovyShell shell = new GroovyShell(groovyLoader, binding, groovyCompilerConfiguration);
					result = shell.evaluate(expression);
				}
			} catch (Exception e) {
				logger.error("An error occurred while evaluation groovy expression " + expression, e);
				throw e;
			}
			long duration = System.currentTimeMillis()-t1;
			
			Integer warnThreshold = Configuration.getInstance().getPropertyAsInteger("tec.expressions.warningthreshold");
			if(warnThreshold!=null && duration > warnThreshold) {
				logger.warn("Groovy-Evaluation of following expression took " + duration + ".ms: "+ expression);
			} else {
				if(logger.isDebugEnabled()) {
					logger.debug("Groovy-Evaluation of following expression took " + duration + ".ms: "+ expression);
				}
			}
			
			if(logger.isDebugEnabled()) {
				logger.debug("Groovy Result:\n" + result);
			}
			
			return result;
		} catch (CompilationFailedException cfe) {
			throw new RuntimeException(
					"Fehler im Groovyausdruck: " + expression, cfe);
		} catch (Exception e){
			throw new RuntimeException(
					"Fehler waehrend Evaluierung: " + expression, e);
		}
	}

}
