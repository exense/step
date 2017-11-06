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
package step.grid.agent.handler;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.agent.AgentTokenServices;

public class MessageHandlerPool {

	private static final Logger logger = LoggerFactory.getLogger(MessageHandlerPool.class);
	
	private final AgentTokenServices tokenServices;
	
	private Map<String, MessageHandler> pool = new HashMap<>();
	
	public MessageHandlerPool(AgentTokenServices tokenServices) {
		super();
		this.tokenServices = tokenServices;
	}

	public synchronized MessageHandler get(String handlerClassname) throws Exception {
		MessageHandler handler = pool.get(handlerClassname); 
		
		if(handler==null) {
			handler = createHandler(handlerClassname);
			pool.put(handlerClassname, handler);
		}
		
		return handler;			
	}

	private MessageHandler createHandler(String handlerClassname) throws ReflectiveOperationException, MalformedURLException,
			ClassNotFoundException, InstantiationException, IllegalAccessException {
		MessageHandler handler;
		try {
			Class<?> class_ = Class.forName(handlerClassname, true, Thread.currentThread().getContextClassLoader());
			handler = newInstance(class_);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw e;
		}		
		
		if (handler instanceof AgentContextAware && tokenServices!=null) {
			((AgentContextAware)handler).init(tokenServices);
		} 
		return handler;
	}

	private MessageHandler newInstance(Class<?> class_)
			throws InstantiationException, IllegalAccessException {
		Object o = class_.newInstance();
		if(o!=null && o instanceof MessageHandler) {
			return (MessageHandler)o;
		} else {
			throw new RuntimeException("The class '"+class_.getName()+"' doesn't extend "+MessageHandler.class);
		}
	}
}
