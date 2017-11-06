package step.grid.agent.tokenpool;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractSession implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(AbstractSession.class);
	
	protected Map<String, Object> sessionObjects = new HashMap<>();

	public AbstractSession() {
		super();
	}

	public Object get(String arg0) {
		return sessionObjects.get(arg0);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> objectClass) {
		Object object = get(objectClass.getName());
		if(object!=null) {
			if(objectClass.isAssignableFrom(object.getClass())) {
				return (T) object;
			} else {
				throw new RuntimeException("The object found is not an instance of "+objectClass.getName());
			}
		} else {
			return null;
		}
	}

	public void put(Object object) {
		put(object.getClass().getName(), object);
	}
	
	public Object put(String arg0, Object arg1) {
		Object previous = get(arg0);
		closeIfCloseable(arg0, previous);
		return sessionObjects.put(arg0, arg1);
	}

	private void closeIfCloseable(String arg0, Object previous) {
		if(previous!=null && previous instanceof Closeable) {
			logger.debug("Attempted to replace session object with key '"+arg0+"'. Closing previous object.");
			try {
				((Closeable)previous).close();
			} catch (Exception e) {
				logger.error("Error while closing '"+arg0+"' from session.",e);
			}
		}
	}

	public void close() {
		for(String key:sessionObjects.keySet()) {
			closeIfCloseable(key, sessionObjects.get(key));
		}
	}

}