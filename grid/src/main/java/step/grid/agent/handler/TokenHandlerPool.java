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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.agent.conf.AgentConf;

public class TokenHandlerPool {

	private static final Logger logger = LoggerFactory.getLogger(TokenHandlerPool.class);
	
	private final AgentConf conf;
	
	private Map<String, MessageHandler> pool = new HashMap<>();
	
	public TokenHandlerPool(AgentConf conf) {
		super();
		this.conf = conf;
	}

	public synchronized MessageHandler get(String handlerKey) throws Exception {
		MessageHandler handler = pool.get(handlerKey); 
		
		if(handler==null) {
			handler = createHandler(handlerKey);
			pool.put(handlerKey, handler);
		}
		
		return handler;			
	}

	private static final String DELIMITER = "\\|";
	
	public MessageHandler createHandler(String handlerChain) throws Exception {
		Iterator<String> handlerKeys = Arrays.asList(handlerChain.split(DELIMITER)).iterator();

		MessageHandler rootHandler = createHandlerRecursive(null, handlerKeys);
		
		return rootHandler;
	}

	private MessageHandler createHandlerRecursive(final MessageHandlerDelegate parent, Iterator<String> handlerKeys) throws Exception {
		final String handlerKey = handlerKeys.next();
		
		MessageHandler handler;
		if(parent!=null) {
			handler = parent.runInContext(new Callable<MessageHandler>() {
				@Override
				public MessageHandler call() throws Exception {
					return createHandler_(handlerKey, parent);
				}	
			});
		} else {
			handler = createHandler_(handlerKey, parent);
		}
		
		if(handlerKeys.hasNext()) {
			if(handler instanceof MessageHandlerDelegate) {
				MessageHandlerDelegate delegator = (MessageHandlerDelegate)handler;
				MessageHandler next = createHandlerRecursive(delegator, handlerKeys);
				delegator.setDelegate(next);
			} else {
				throw new RuntimeException("The handler '"+handlerKey+"' should implement the interface MessageHandlerDelegate if used in an handler chain.");
			}
		}
		
		return handler;
	}

	private MessageHandler createHandler_(String handlerKey, MessageHandlerDelegate previous) throws ReflectiveOperationException, MalformedURLException,
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
			} else if (factory.equals("classuri")) {	
				URL[] urls = classPathStringToURLs(factoryKey);			
				ClassLoader cl = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
				handler = new ClassLoaderMessageHandlerWrapper(cl);
			} else if (factory.equals("isolatedClasspath")) {	
				URL[] urls = classPathStringToURLs(factoryKey);			
				ClassLoader cl = new IsolatingURLClassLoader(urls, Thread.currentThread().getContextClassLoader());
				handler = new ClassLoaderMessageHandlerWrapper(cl);				
			} else {
				throw new RuntimeException("Unknown handler factory: "+factory);
			}
				
		} else {
			throw new RuntimeException("Invalid handler key: "+handlerKey);
		}
		
		if(handler instanceof AgentConfigurationAware && conf!=null) {
			((AgentConfigurationAware)handler).init(conf);
		}
		return handler;
	}

	private URL[] classPathStringToURLs(String factoryKey) throws MalformedURLException {
		List<URL> urls = new ArrayList<>();
		
		String[] paths = factoryKey.split(";");
		for(String path:paths) {
			File f = new File(path);
			addJarsToUrls(urls, f);
			try {
				urls.add(f.getCanonicalFile().toURI().toURL());
			} catch (IOException e) {
				logger.warn("Error while getting canonical file name of '"+f.getPath()+"'",e );
			}
		}
		return urls.toArray(new URL[urls.size()]);
	}

	public static void addJarsToUrls(List<URL> urls, File f) throws MalformedURLException {
		if(f.isDirectory()) {
			for(File file:f.listFiles()) {
				if(file.isDirectory()) {
					addJarsToUrls(urls, file);
				} else {
					if(file.getName().endsWith(".jar")) {
						urls.add(file.toURI().toURL());
					}					
				}
			}
		}
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
