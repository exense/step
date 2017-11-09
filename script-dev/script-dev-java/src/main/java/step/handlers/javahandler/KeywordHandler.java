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
import java.net.URLClassLoader;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;
import step.grid.isolation.ApplicationContextBuilder.ApplicationContext;

public class KeywordHandler extends AbstractMessageHandler {
	
	public static final String KEYWORD_CLASSES = "$keywordClasses";
	
	private static final Logger logger = LoggerFactory.getLogger(KeywordHandler.class);

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		ApplicationContext context = agentTokenServices.getApplicationContextBuilder().getCurrentContext();
		URLClassLoader cl = (URLClassLoader) context.getClassLoader();
		
		String kwClassnames = message.getProperties().get(KEYWORD_CLASSES);
		for(String kwClassname:kwClassnames.split(";")) {
			Class<?> kwClass = cl.loadClass(kwClassname);
			
			for (Method m : kwClass.getDeclaredMethods()) {
				if(m.isAnnotationPresent(Keyword.class)) {
					Keyword annotation = m.getAnnotation(Keyword.class);
					String annotatedFunctionName = annotation.name();
					if (((annotatedFunctionName == null || annotatedFunctionName.length() == 0)
							&& m.getName().equals(message.getFunction()))
							|| annotatedFunctionName.equals(message.getFunction())) {
						return invokeMethod(m, token, message);
					}		
				}
			}			
		}

		throw new Exception("Unable to find method annoted by '" + Keyword.class.getName() + "' with name=='"+ message.getFunction() + "'");
	}

	private OutputMessage invokeMethod(Method m, AgentTokenWrapper token, InputMessage message)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException,
			IllegalArgumentException, NoSuchMethodException, SecurityException {
		Class<?> clazz = m.getDeclaringClass();
		Object instance = clazz.newInstance();

		if (logger.isDebugEnabled()) {
			logger.debug("Invoking method " + m.getName() + " from class " + clazz.getName() + " loaded by "
					+ clazz.getClassLoader().toString());
		}

		Map<String, String> properties = buildPropertyMap(token, message);

		if (logger.isDebugEnabled()) {
			logger.debug("Using property map: " + properties.toString());
		}

		// AbstractScript script = null;
		OutputMessageBuilder output = new OutputMessageBuilder();


		if (instance instanceof AbstractKeyword) {
			AbstractKeyword script = (AbstractKeyword) instance;
			script.setTokenSession(token.getSession());
			script.setSession(token.getTokenReservationSession());
			script.setInput(message.getArgument());
			script.setProperties(properties);
			script.setOutputBuilder(output);

			try {
				m.invoke(instance);
			} catch (Exception e) {
				boolean throwException = script.onError(e);
				if (throwException) {
					throw e;
				}
			} finally {
				// TODO error handling
			}
		} else {
			output.add("Info:", "The class '" + clazz.getName() + "' doesn't extend '" + AbstractKeyword.class.getName()
					+ "'. Extend this class to get input parameters from STEP and return output.");
		}
		return output.build();
	}
}
