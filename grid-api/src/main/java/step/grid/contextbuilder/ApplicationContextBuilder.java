package step.grid.contextbuilder;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.io.InputMessage;

public class ApplicationContextBuilder {
	
	private static final Logger logger = LoggerFactory.getLogger(ApplicationContextBuilder.class);
		
	private ApplicationContext rootContext;
		
	private ThreadLocal<ApplicationContext> currentContexts = new ThreadLocal<>();
	
	public static class ApplicationContext implements Closeable {
		
		private ClassLoader classLoader;

		private Map<String, ApplicationContext> childContexts = new ConcurrentHashMap<>();
		
		private Map<String, Object> contextObjects = new HashMap<>();

		public ApplicationContext() {
			super();
		}

		public Object get(Object key) {
			return contextObjects.get(key);
		}

		public Object put(String key, Object value) {
			return contextObjects.put(key, value);
		}

		public ClassLoader getClassLoader() {
			return classLoader;
		}

		@Override
		public void close() throws IOException {
			if(classLoader!=null && classLoader instanceof Closeable) {
				((Closeable)classLoader).close();
			}
		}
	}
	
	public ApplicationContextBuilder() {
		rootContext = new ApplicationContext();
		rootContext.classLoader = InputMessage.class.getClassLoader();
		resetContext();
	}
	
	public void resetContext() {
		currentContexts.set(rootContext);
	}
	
	public void pushContext(ApplicationContextFactory descriptor) {
		synchronized(this) {
			ApplicationContext parentContext = currentContexts.get();
			if(parentContext==null) {
				parentContext = rootContext;
			}
			
			String contextKey = descriptor.getId();
			ApplicationContext context;
			if(!parentContext.childContexts.containsKey(contextKey)) {
				context = new ApplicationContext();
				buildClassLoader(descriptor, context, parentContext);
				parentContext.childContexts.put(contextKey, context);
			} else {
				context = parentContext.childContexts.get(contextKey);	
				if(descriptor.requiresReload()) {
					buildClassLoader(descriptor, context, parentContext);
					context.contextObjects.clear();
				} else {
					
				}
			}
			currentContexts.set(context);
		}
	}

	private void buildClassLoader(ApplicationContextFactory descriptor, ApplicationContext context,	ApplicationContext parentContext) {
		ClassLoader classLoader = descriptor.buildClassLoader(parentContext.classLoader);
		context.classLoader = classLoader;
	}
	
	public ApplicationContext getCurrentContext() {
		return currentContexts.get();
	}
	
	public <T> T runInContext(Callable<T> runnable) throws Exception {
		ClassLoader previousCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(getCurrentContext().getClassLoader());
		try {
			return runnable.call();
		} finally {
			Thread.currentThread().setContextClassLoader(previousCl);
		}
	}
}
