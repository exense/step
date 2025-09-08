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
package step.expressions;

import java.util.*;
import java.util.Map.Entry;

import groovy.lang.*;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static step.expressions.ProtectionAwareGroovySetup.setupProtectionAwareOperations;

public class ExpressionHandler implements AutoCloseable {
		
	private static final Logger logger = LoggerFactory.getLogger(ExpressionHandler.class);
	
	private final GroovyPool groovyPool;
	
	private final Integer executionTimeWarningTreshold;
	
	private final String scriptBaseClass;

	public ExpressionHandler() {
		this(null);
	}
	
	public ExpressionHandler(String scriptBaseClass) {
		this(scriptBaseClass, null, 1000, 8, -1, null);
	}
	
	public ExpressionHandler(String scriptBaseClass, Integer executionTimeWarningTreshold, int poolMaxTotal,  int poolMaxTotalPerKey, int poolMaxIdlePerKey, Integer monitoringIntervalSeconds) {
		this(scriptBaseClass, null, executionTimeWarningTreshold, poolMaxTotal, poolMaxTotalPerKey, poolMaxIdlePerKey, monitoringIntervalSeconds);
	}

	// Only used directly to pass a custom groovyPoolFactory in Junit test
	protected ExpressionHandler(String scriptBaseClass, GroovyPoolFactory groovyPoolFactory, Integer executionTimeWarningTreshold, int poolMaxTotal,  int poolMaxTotalPerKey, int poolMaxIdlePerKey, Integer monitoringIntervalSeconds) {
		super();
		this.scriptBaseClass = scriptBaseClass;
		this.groovyPool = (groovyPoolFactory != null) ? new GroovyPool(groovyPoolFactory, poolMaxTotal, poolMaxTotalPerKey, poolMaxIdlePerKey, monitoringIntervalSeconds) :
				new GroovyPool(scriptBaseClass, poolMaxTotal, poolMaxTotalPerKey, poolMaxIdlePerKey, monitoringIntervalSeconds);
		this.executionTimeWarningTreshold = executionTimeWarningTreshold;
		setupProtectionAwareOperations();
	}

	public Object evaluateGroovyExpression(String expression, Map<String, Object> bindings) {
		return evaluateGroovyExpression(expression, bindings, false);
	}

	/**
	 * Evaluate a groovy expression using provided binding. Not that special binding of type ProtectedBinding will be handled depending on the granted access right
	 * @param expression the groovy expression to be evaluated
	 * @param bindings the map of bindings (variables) available for the evaluation
	 * @param canAccessProtectedValue whether protected values provided as ProtectedBinding can be access. Accessing such binding with no access will throw an exception
	 * @return the result of the groovy evaluation
	 */
	public Object evaluateGroovyExpression(String expression, Map<String, Object> bindings, boolean canAccessProtectedValue) {
		try {
			Object result;
			Set<String> excludedProtectedBindingKeys = new HashSet<>();
			try {
				if(logger.isDebugEnabled()) {
					logger.debug("Groovy evaluation:\n" + expression);
				}

				// Set the protection context
				ProtectionContext.set(canAccessProtectedValue);
				
				Binding binding = new Binding();
				if(bindings!=null) {
					for(Entry<String, Object> varEntry : bindings.entrySet()) {
						String key = varEntry.getKey();
						Object value =  varEntry.getValue();
						if (!canAccessProtectedValue && value instanceof ProtectedBinding) {
							excludedProtectedBindingKeys.add(key);
						} else {
							binding.setVariable(key, value);
						}
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
				} catch (MultipleCompilationErrorsException e) {
					for (Object error : e.getErrorCollector().getErrors()) {
						if(error instanceof SyntaxErrorMessage) {
							String message = ((SyntaxErrorMessage) error).getCause().getMessage();
							if(message != null && message.contains("unable to resolve class") && scriptBaseClass != null && message.contains(scriptBaseClass)) {
								throw new Exception("Unable to resolve groovy macro class '" + scriptBaseClass + 
										"'. Please ensure that the groovy script containing your custom macros is available in the classpath.", e);
							}
						}
					}
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

				// Handle GString results that contain ProtectedBinding
				if (result instanceof GString) {
					result = ProtectionAwareGroovySetup.handleGStringWithProtectedBindings((GString) result);
				}
				if(logger.isDebugEnabled()) {
					logger.debug("Groovy result:\n" + result);
				}
				return result;
			} catch (CompilationFailedException cfe) {
				throw new RuntimeException(
						"Error while compiling groovy expression: '" + expression + "'", cfe);
			} catch (MissingPropertyException mpe) {
				String property = mpe.getProperty();
				String baseMessage = "Error while resolving groovy properties in expression: '" + expression + "'. ";
				if (excludedProtectedBindingKeys.contains(property)) {
					throw new RuntimeException(baseMessage + "The property '" + property + "' is protected and can only be used as a Keyword input or property.");
				} else {
					throw new RuntimeException(
							baseMessage + "The property '" + property + "' could not be found (or accessed). Make sure that the property is defined as variable or parameter and is accessible in the current scope.");
				}
			} catch (Exception e){
				throw new RuntimeException(
						"Error while running groovy expression: '" + expression + "'", e);
			}
		} catch (Exception e) {
			if(logger.isDebugEnabled()) {
				logger.error("An error occurred while evaluation groovy expression " + expression, e);
			}
			throw e;
		} finally {
			// Always clear the context
			ProtectionContext.clear();
		}
	}

	public static Object checkProtectionAndWrapIfRequired(boolean isParentProtected, Object value, String key) {
		if (isParentProtected) {
			return new ProtectedBinding(value, key);
		}
		return value;
	}


	@Override
	public void close() {
		groovyPool.close();
	}
}
