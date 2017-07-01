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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.agent.handler.MessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;
import step.handlers.scripthandler.ScriptHandler;

public class JavaHandler implements MessageHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(JavaHandler.class);
	
	private Map<String, JavaExecutionContext> executionContexts = new ConcurrentHashMap<>();
	
	class JavaExecutionContext {
		
		Long fileVersion;
		
		ClassLoader classLoader;
		
		Reflections reflections;
	}
	
	public JavaHandler() {
		super();
	}

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		
		String transferFileId = message.getProperties().get(ScriptHandler.REMOTE_FILE_ID);
		long transferFileVersion = Long.parseLong(message.getProperties().get(ScriptHandler.REMOTE_FILE_VERSION));
		
		if(logger.isDebugEnabled()) {
			logger.debug("Getting java context for transfer file with id '"+transferFileId+"' and version '"+transferFileVersion+"'");
		}
		
		JavaExecutionContext context = executionContexts.get(transferFileId);
		
		if(context==null) {
			if(logger.isDebugEnabled()) {
				logger.debug("Creating new java context for transfer file with id '"+transferFileId+"'");
			}
			
			context = new JavaExecutionContext();
			JavaExecutionContext currentValue = executionContexts.putIfAbsent(transferFileId, context);
			if(currentValue!=null) {
				context = currentValue;
			}
		}
			
		synchronized (context) {
			if(context.fileVersion==null || context.fileVersion<transferFileVersion) {			
				if(logger.isDebugEnabled()) {
					logger.debug("Requesting transfer file with id '"+transferFileId+"' and version '"+transferFileVersion+"'");
				}
				
				File transferFile = token.getServices().getFileManagerClient().requestFile(transferFileId, transferFileVersion);
				
				URL scriptUrl = transferFile.toURI().toURL();
				context.fileVersion = transferFileVersion;
				
				URLClassLoader classLoader = new URLClassLoader(new URL[]{scriptUrl}, Thread.currentThread().getContextClassLoader());
				Reflections reflections = new Reflections(
						new ConfigurationBuilder().setUrls(scriptUrl).addClassLoader(classLoader).setScanners(new MethodAnnotationsScanner()));
				context.classLoader = classLoader;
				context.reflections = reflections;
				
				if(logger.isDebugEnabled()) {
					logger.debug("Created java context for fileid '"+transferFileId+"'. URLs: "+scriptUrl.toString());
				}
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

		if(logger.isDebugEnabled()) {
			logger.debug("Invoking method " + m.getName() + " from class "+clazz.getName()+" loaded by "+clazz.getClassLoader().toString());
		}
		
		Map<String, String> properties = buildPropertyMap(token, message);
		
		if(logger.isDebugEnabled()) {
			logger.debug("Using property map: "+properties.toString());
		}
		
		AbstractScript script = null;
		OutputMessageBuilder output = new OutputMessageBuilder();
		if(instance instanceof AbstractScript) {
			script = (AbstractScript) instance;
			script.setTokenSession(token.getSession());
			script.setSession(token.getTokenReservationSession());
			script.setInput(message.getArgument());
			script.setProperties(properties);
			script.setOutputBuilder(output);
			
		} else {
			output.add("Info:", "The class '"+clazz.getName()+"' doesn't extend '"+AbstractScript.class.getName()+"'. Extend this class to get input parameters from STEP and return output.");
		}
		try {
			m.invoke(instance);
		} catch(Exception e) {
			boolean throwException = script.onError(e);
			if(throwException) {
				throw e;
			}
		} finally {
			// TODO error handling
		}
		return output.build();
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
