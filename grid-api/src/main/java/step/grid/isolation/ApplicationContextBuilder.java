package step.grid.isolation;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.bootstrap.RemoteClassPathBuilder.RemoteClassPath;
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
	}
	
	public void resetContext() {
		currentContexts.set(null);
	}
	
	public void pushContextIfAbsent(String contextKey, RemoteClassPath remoteClassPath) {
		synchronized (this) {
			ApplicationContext parentContext = currentContexts.get();
			if(parentContext==null) {
				parentContext = rootContext;
			}

			ApplicationContext context;
			if(!parentContext.childContexts.containsKey(contextKey)) {
				context = createNewApplicationContext(contextKey, parentContext, remoteClassPath);
			} else {
				ApplicationContext currentContext = parentContext.childContexts.get(contextKey);					
				if(remoteClassPath.isForceReload()) {
					try {
						currentContext.close();
					} catch (IOException e) {
						logger.warn("Error while closing context", e);
					}
					context = createNewApplicationContext(contextKey, parentContext, remoteClassPath);
				} else {
					context = currentContext;
				}
			}
			currentContexts.set(context);
		}	
	}
	
	public void pushContextIfAbsent(String contextKey, Supplier<RemoteClassPath> remoteClassPathSupplier) {
		synchronized (this) {
			ApplicationContext parentContext = currentContexts.get();
			if(parentContext==null) {
				parentContext = rootContext;
			}

			ApplicationContext context;
			if(!parentContext.childContexts.containsKey(contextKey)) {
				RemoteClassPath remoteClassPath = remoteClassPathSupplier.get();
				context = createNewApplicationContext(contextKey, parentContext, remoteClassPath);
			} else {
				context = parentContext.childContexts.get(contextKey);
			}
			currentContexts.set(context);
		}	
	}

	private ApplicationContext createNewApplicationContext(String contextKey, ApplicationContext parentContext,
			RemoteClassPath remoteClassPath) {
		ApplicationContext context;
		context = new ApplicationContext();
		URL[] urlArray = remoteClassPath.getUrls().toArray(new URL[remoteClassPath.getUrls().size()]);
		@SuppressWarnings("resource")
		ClassLoader classLoader = new URLClassLoader(urlArray, parentContext.classLoader);
		context.classLoader = classLoader;
		parentContext.childContexts.put(contextKey, context);
		return context;
	}
	
	public ApplicationContext getCurrentContext() {
		return currentContexts.get();
	}
}
