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
package step.handlers.javahandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;

public class KeywordHandler extends JsonBasedFunctionHandler {
	
	public static final String KEYWORD_CLASSES = "$keywordClasses";
	
	private static final Logger logger = LoggerFactory.getLogger(KeywordHandler.class);
	
	private boolean throwExceptionOnError = false;

	public KeywordHandler() {
		super();
	}

	public KeywordHandler(boolean throwExceptionOnError) {
		super();
		this.throwExceptionOnError = throwExceptionOnError;
	}

	public boolean isThrowExceptionOnError() {
		return throwExceptionOnError;
	}

	public void setThrowExceptionOnError(boolean throwExceptionOnError) {
		this.throwExceptionOnError = throwExceptionOnError;
	}

	@Override
	public Output<JsonObject> handle(Input<JsonObject> input) throws Exception {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		
		String kwClassnames = input.getProperties().get(KEYWORD_CLASSES);
		if(kwClassnames != null && kwClassnames.trim().length()>0) {
			for(String kwClassname:kwClassnames.split(";")) {
				Class<?> kwClass = cl.loadClass(kwClassname);
				
				for (Method m : kwClass.getDeclaredMethods()) {
					if(m.isAnnotationPresent(Keyword.class)) {
						Keyword annotation = m.getAnnotation(Keyword.class);
						String annotatedFunctionName = annotation.name();
						if (((annotatedFunctionName == null || annotatedFunctionName.length() == 0)
								&& m.getName().equals(input.getFunction()))
								|| annotatedFunctionName.equals(input.getFunction())) {
							return invokeMethod(m, getToken(), input);
						}		
					}
				}			
			}
		}

		throw new Exception("Unable to find method annoted by '" + Keyword.class.getName() + "' with name=='"+ input.getFunction() + "'");
	}

	private Output<JsonObject> invokeMethod(Method m, AgentTokenWrapper token, Input<JsonObject> input)
			throws Exception {
		Class<?> clazz = m.getDeclaringClass();
		Object instance = clazz.newInstance();

		if (logger.isDebugEnabled()) {
			logger.debug("Invoking method " + m.getName() + " from class " + clazz.getName() + " loaded by "
					+ clazz.getClassLoader().toString());
		}

		OutputBuilder outputBuilder = new OutputBuilder();

		if (instance instanceof AbstractKeyword) {
			AbstractKeyword script = (AbstractKeyword) instance;
			script.setTokenSession(token.getSession());
			script.setSession(token.getTokenReservationSession());
			script.setInput(input.getPayload());
			script.setProperties(mergeAllProperties(input));
			script.setOutputBuilder(outputBuilder);

			try {
				m.invoke(instance);
			} catch (Exception e) {
				boolean throwException = script.onError(e);
				if (throwException) {
					Throwable cause = e.getCause();
					Throwable reportedEx;
					if(e instanceof InvocationTargetException && cause!=null && cause instanceof Throwable) {
						reportedEx = cause;
					} else {
						reportedEx = e;
					}
					outputBuilder.setError(reportedEx.getMessage()!=null?reportedEx.getMessage():"Empty error message", reportedEx);
					if(throwExceptionOnError) {
						Output<?> output = outputBuilder.build();
						throw new KeywordException(output, reportedEx);
					}
				}
			} finally {
				// TODO error handling
			}
		} else {
			outputBuilder.add("Info:", "The class '" + clazz.getName() + "' doesn't extend '" + AbstractKeyword.class.getName()
					+ "'. Extend this class to get input parameters from STEP and return output.");
		}
		
		Output<JsonObject> output = outputBuilder.build();
		if(throwExceptionOnError && output.getError() != null) {
			throw new KeywordException(output);
		} else {
			return output;
		}
	}
}
