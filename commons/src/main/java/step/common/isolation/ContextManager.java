package step.common.isolation;

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
		
	private static ThreadLocal<ExecutionContext> currentContexts = new ThreadLocal<>();
	
	private Map<String, ExecutionContext> contexts = new ConcurrentHashMap<>();
	
	private class ExecutionContext {
		
		ClassLoader contextClassLoader;
		
		Object contextObject;

		public ExecutionContext(ClassLoader classLoader) {
			super();
			this.contextClassLoader = classLoader;
		}
	}
	
	public static Object getCurrentContextObject() {
		return currentContexts.get().contextObject;
	}
	
	public void loadContext(String contextKey, List<URL> urls, boolean isolatedContext) throws Exception {
		loadContext(contextKey, urls, isolatedContext, null);
	}
	
	public void loadContext(String contextKey, List<URL> urls, boolean isolatedContext, Callable<Object> contextObjectFactory) throws Exception {
		logger.info("Loading context "+contextKey+" with URLs "+urls.toString());
		
		ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
		URL[] urlArray = urls.toArray(new URL[urls.size()]);
		
		ClassLoader classLoader;
		if(isolatedContext) {
			classLoader = new IsolatingURLClassLoader(urlArray, parentClassLoader);
		} else {
			classLoader = new URLClassLoader(urlArray, parentClassLoader);
		}
		
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

	private <T> T runInContext(Callable<T> runnable, ExecutionContext context) throws Exception {
		ClassLoader previousCl = Thread.currentThread().getContextClassLoader();
		
		Thread.currentThread().setContextClassLoader(context.contextClassLoader);
		currentContexts.set(context);
		try {
			return runnable.call();
		} finally {
			currentContexts.remove();
			Thread.currentThread().setContextClassLoader(previousCl);
		}
	}
}
