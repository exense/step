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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.agent.AgentTokenServices;

public class TokenHandlerPool {

	private static final Logger logger = LoggerFactory.getLogger(TokenHandlerPool.class);
	
	private final AgentTokenServices tokenServices;
	
	private Map<String, MessageHandler> pool = new HashMap<>();
	
	public TokenHandlerPool(AgentTokenServices tokenServices) {
		super();
		this.tokenServices = tokenServices;
	}

	public synchronized MessageHandler get(String handlerKey) throws Exception {
		MessageHandler handler = pool.get(handlerKey); 
		
		if(handler==null) {
			handler = createHandler(handlerKey);
			pool.put(handlerKey, handler);
		}
		
		return handler;			
	}

	private MessageHandler createHandler(String handlerKey) throws ReflectiveOperationException, MalformedURLException,
			ClassNotFoundException, InstantiationException, IllegalAccessException {
		MessageHandler handler;
		Matcher m = HANDLER_KEY_PATTERN.matcher(handlerKey);
		if(m.matches()) {
			String factory = m.group(1);
			String factoryKey = m.group(2);
			
			if(factory.equals("class")) {
				try {
					Class<?> class_ = Class.forName(factoryKey, true, Thread.currentThread().getContextClassLoader());
					handler = newInstance(class_);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					throw e;
				}
			} else {
				throw new RuntimeException("Unknown handler factory: "+factory);
			}
				
		} else {
			throw new RuntimeException("Invalid handler key: "+handlerKey);
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
	
	private static final Pattern HANDLER_KEY_PATTERN = Pattern.compile("(.+?):(.+?)");
}
