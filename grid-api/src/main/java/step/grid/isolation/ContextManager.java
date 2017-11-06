package step.grid.isolation;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextManager {
	
	private static final Logger logger = LoggerFactory.getLogger(ContextManager.class);
			
	private Map<String, ExecutionContext> contexts = new ConcurrentHashMap<>();
	
	private ThreadLocal<ExecutionContext> currentContext = new ThreadLocal<>();
	
	private class ExecutionContext {
				
		ClassLoader contextClassLoader;
		
		Object contextObject;

		public ExecutionContext(ClassLoader classLoader) {
			super();
			this.contextClassLoader = classLoader;
		}
	}
	
	public void loadContextIfAbsent(String contextKey, List<URL> urls, Callable<Object> contextObjectFactory, boolean forceReload) {
		synchronized (this) {
			if (!contexts.containsKey(contextKey) || forceReload) {
				try {
					loadContext(contextKey, urls, contextObjectFactory);
				} catch (Exception e1) {
					throw new RuntimeException("Error while loading context", e1);
				}
			}
		}	
	}
	
	protected void loadContext(String contextKey, List<URL> urls, Callable<Object> contextObjectFactory) throws Exception {
		logger.info("Loading context "+contextKey+" with URLs "+urls.toString());
		
		URL[] urlArray = urls.toArray(new URL[urls.size()]);
		
		ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader	classLoader = new URLClassLoader(urlArray, parentClassLoader);
		ExecutionContext context = new ExecutionContext(classLoader);
		if(contextObjectFactory!=null) {
			context.contextObject = runInContext(contextObjectFactory, context);			
		}
		contexts.put(contextKey, context);
		
	}
	
	public <T> T runInContext(String contextKey, Callable<T> runnable) throws Exception {
		ExecutionContext context = contexts.get(contextKey);
		
		if(context!=null) {
			return runInContext(runnable, context);			
		} else {
			throw new RuntimeException("Unable to find context with key: '"+contextKey+"'.");
		}
	}

	public Object getCurrentContextObject() {
		ExecutionContext context = currentContext.get();
		return context.contextObject;
	}
	
	private <T> T runInContext(Callable<T> runnable, ExecutionContext context) throws Exception {
		ClassLoader previousCl = Thread.currentThread().getContextClassLoader();
		
		currentContext.set(context);
		Thread.currentThread().setContextClassLoader(context.contextClassLoader);
		try {
			return runnable.call();
		} finally {
			Thread.currentThread().setContextClassLoader(previousCl);
			currentContext.set(null);
		}
	}
}
