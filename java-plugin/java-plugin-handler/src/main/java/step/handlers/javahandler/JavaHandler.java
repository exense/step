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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Set;

import javax.json.JsonObject;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.agent.tokenpool.TokenSession;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;
import step.grid.isolation.ApplicationContextBuilder.ApplicationContext;

public class JavaHandler extends AbstractMessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(JavaHandler.class);

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		ApplicationContext context = agentTokenServices.getApplicationContextBuilder().getCurrentContext();
		URLClassLoader cl = (URLClassLoader) context.getClassLoader();
		@SuppressWarnings("unchecked")
		Set<Method> methods = (Set<Method>) context.get("methods");
		if (methods == null) {
			URL url = cl.getURLs()[0];
			try {
				Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(url)
						.addClassLoader(cl).setScanners(new MethodAnnotationsScanner()));
				methods = reflections.getMethodsAnnotatedWith(Keyword.class);
				context.put("methods", methods);
			} catch (Exception e) {
				String errorMsg = "Error while looking for methods annotated with @Keyword in "+url.toString();
				logger.error(errorMsg, e);
				throw new Exception(errorMsg, e);
			}
		}
		
		// Working with reflection as the class Keyword is loaded in both the classloader of the ApplicationContext and by the classloader of the JavaHandler
		@SuppressWarnings("unchecked")
		Class<? extends Annotation> kwClass = (Class<? extends Annotation>) cl.loadClass(Keyword.class.getName());
		Method getName = kwClass.getMethod("name");
		for (Method m : methods) {
			Annotation annotation = m.getAnnotation(kwClass);
			String annotatedFunctionName = (String) getName.invoke(annotation);

			if (((annotatedFunctionName == null || annotatedFunctionName.length() == 0)
					&& m.getName().equals(message.getFunction()))
					|| annotatedFunctionName.equals(message.getFunction())) {
				return invokeMethod(cl, m, token, message);
			}
		}

		throw new Exception("Unable to find method annoted by '" + Keyword.class.getName() + "' with name=='"+ message.getFunction() + "'");
	}

	private OutputMessage invokeMethod(ClassLoader cl, Method m, AgentTokenWrapper token, InputMessage message)
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

		Class<?> abstractScriptClass = cl.loadClass(AbstractScript.class.getName());

		// Working with reflection as the class AbstractScript is loaded in both the classloader of the ApplicationContext and by the classloader of the JavaHandler
		if (abstractScriptClass.isAssignableFrom(instance.getClass())) {
			abstractScriptClass.getMethod("setTokenSession", TokenSession.class).invoke(instance, token.getSession());
			abstractScriptClass.getMethod("setSession", TokenReservationSession.class).invoke(instance,
					token.getTokenReservationSession());
			abstractScriptClass.getMethod("setInput", JsonObject.class).invoke(instance, message.getArgument());
			abstractScriptClass.getMethod("setProperties", Map.class).invoke(instance, properties);
			abstractScriptClass.getMethod("setOutputBuilder", OutputMessageBuilder.class).invoke(instance, output);
			// script = (AbstractScript) instance;
			// script.setTokenSession(token.getSession());
			// script.setSession(token.getTokenReservationSession());
			// script.setInput(message.getArgument());
			// script.setProperties(properties);
			// script.setOutputBuilder(output);

		} else {
			output.add("Info:", "The class '" + clazz.getName() + "' doesn't extend '" + AbstractScript.class.getName()
					+ "'. Extend this class to get input parameters from STEP and return output.");
		}
		try {
			m.invoke(instance);
		} catch (Exception e) {
			boolean throwException = (boolean) abstractScriptClass.getMethod("onError", Exception.class)
					.invoke(instance, e);

			// boolean throwException = script.onError(e);
			if (throwException) {
				throw e;
			}
		} finally {
			// TODO error handling
		}
		return output.build();
	}

	// Reflections reflections;
	//
	// Set<Method> methods;
	//
	// public void init(Class<?> clazz) {
	// final String className = clazz.getCanonicalName();
	// final Predicate<String> filter = new Predicate<String>() {
	// public boolean apply(String arg0) {
	// return arg0.startsWith(className);
	// }
	// };
	// reflections = new Reflections(new
	// ConfigurationBuilder().setUrls(ClasspathHelper.forClass(clazz)).filterInputsBy(filter).setScanners(new
	// MethodAnnotationsScanner()));
	// methods = reflections.getMethodsAnnotatedWith(Keyword.class);
	// }
	//
	// public void init() {
	// reflections = new Reflections(new
	// ConfigurationBuilder().setUrls(ClasspathHelper.forClassLoader(Thread.currentThread().getContextClassLoader())).setScanners(new
	// MethodAnnotationsScanner()));
	// methods = reflections.getMethodsAnnotatedWith(Keyword.class);
	//
	// }

}
