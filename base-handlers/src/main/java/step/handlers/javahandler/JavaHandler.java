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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import step.grid.agent.handler.MessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class JavaHandler implements MessageHandler {
	
	private Map<String, JavaExecutionContext> executionContexts = new ConcurrentHashMap<>();
	
	class JavaExecutionContext {
		
		Long fileVersion;
		
		ClassLoader classLoader;
		
		Reflections reflections;
	}
	
	public static final String REMOTE_FILE_ID = "remotefile.id";
	public static final String REMOTE_FILE_VERSION = "remotefile.version";
	
	public JavaHandler() {
		super();
	}

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		
		String transferFileId = message.getProperties().get(REMOTE_FILE_ID);
		long transferFileVersion = Long.parseLong(message.getProperties().get(REMOTE_FILE_VERSION));
		
		JavaExecutionContext context = executionContexts.get(transferFileId);
		
		if(context==null) {
			context = new JavaExecutionContext();
			JavaExecutionContext currentValue = executionContexts.putIfAbsent(transferFileId, context);
			if(currentValue!=null) {
				context = currentValue;
			}
		}
			
		synchronized (context) {
			if(context.fileVersion==null || context.fileVersion<transferFileVersion) {			
				File transferFile = token.getServices().getFileManagerClient().requestFile(message.getProperties().get(REMOTE_FILE_ID), Long.parseLong(message.getProperties().get(REMOTE_FILE_VERSION)));
				URL scriptUrl = transferFile.toURI().toURL();
				context.fileVersion = transferFileVersion;
				
				URLClassLoader classLoader = new URLClassLoader(new URL[]{scriptUrl}, Thread.currentThread().getContextClassLoader());
				Reflections reflections = new Reflections(
						new ConfigurationBuilder().setUrls(scriptUrl).addClassLoader(classLoader).setScanners(new MethodAnnotationsScanner()));
				context.classLoader = classLoader;
				context.reflections = reflections;
			}			
		}
		
		Set<Method> methods = context.reflections.getMethodsAnnotatedWith(Function.class);
		for(Method m:methods) {
			String annotatedFunctionName = m.getAnnotation(Function.class).name();
			if(((annotatedFunctionName==null || annotatedFunctionName.length()==0)&&m.getName().equals(message.getFunction()))||
					m.getAnnotation(Function.class).name().equals(message.getFunction())) {
				return invokeMethod(m, token, message);
			}
		}
		throw new Exception("Unable to find method annoted by '"+Function.class.getName()+"' with name=='"+message.getFunction()+"'");
		
	}

	private OutputMessage invokeMethod(Method m, AgentTokenWrapper token, InputMessage message)
			throws InstantiationException, IllegalAccessException, InvocationTargetException {
		Class<?> clazz = m.getDeclaringClass();
		Object instance = clazz.newInstance();

		Map<String, String> properties = buildPropertyMap(token, message);
		
		AbstractScript script = null;
		if(instance instanceof AbstractScript) {
			script = (AbstractScript) instance;
			script.setSession(token.getSession());
			script.setInput(message.getArgument());
			script.setProperties(properties);
			OutputMessageBuilder output = new OutputMessageBuilder();
			script.setOutputBuilder(output);
			try {
				m.invoke(instance);
			} finally {
				// TODO error handling
			}
			
			return output.build();
		} else {
			throw new RuntimeException("The class '"+clazz.getName()+"' doesn't extend '"+AbstractScript.class.getName()+"'");
		}
	}

	private Map<String, String> buildPropertyMap(AgentTokenWrapper token, InputMessage message) {
		Map<String, String> properties = new HashMap<>();
		if(message.getProperties()!=null) {
			properties.putAll(message.getProperties());
		}
		if(token.getProperties()!=null) {
			properties.putAll(token.getProperties());			
		}
		return properties;
	}

}
