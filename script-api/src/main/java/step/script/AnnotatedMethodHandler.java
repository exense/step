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
package step.script;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import step.grid.agent.handler.MessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class AnnotatedMethodHandler implements MessageHandler {

	boolean alwaysThrowExceptions;
	
	Reflections reflections;
	
	public AnnotatedMethodHandler() {
		this(false);
	}

	public AnnotatedMethodHandler(boolean alwaysThrowExceptions) {
		super();
		this.alwaysThrowExceptions = alwaysThrowExceptions;
		reflections = new Reflections(new ConfigurationBuilder()
	            .setUrls(ClasspathHelper.forPackage("", Thread.currentThread().getContextClassLoader()))
	            .setScanners(new MethodAnnotationsScanner()));
	}

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {		
		Set<Method> jobSubTypes = reflections.getMethodsAnnotatedWith(Function.class);
		for(Method m:jobSubTypes) {
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
			script.beforeCall(token, message);
			try {
				AnnotatedMethodInvoker.invoke(instance, m, message.getArgument().toString(), properties);
			} catch(Exception e) {
				boolean errorHandled = script.onError(token, message, e);
				if(!alwaysThrowExceptions && errorHandled) {
					// Nothing to be done here as the exception has already be handled by the script
				} else {
					throw e;
				}
			} finally {
				if(script!=null) {
					script.afterCall(token, message);							
				}
			}
			
			OutputMessageBuilder outputBuilder = script.outputBuilder;
			return outputBuilder.build();
		} else {
			if(m.getReturnType() == OutputMessage.class) {
				Object result = AnnotatedMethodInvoker.invoke(instance, m, message.getArgument().toString(), properties);
				return result!=null?(OutputMessage) result:null;
			} else {
				throw new RuntimeException("The method '"+m.getName()+"' from class '"+clazz.getName()+
						"' neither extend "+AbstractScript.class.getName()+" nor returns an "+OutputMessage.class.getName());				
			}
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
