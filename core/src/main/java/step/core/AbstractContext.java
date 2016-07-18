package step.core;

import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractContext {

	private final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	public Object get(Object key) {
		return attributes.get(key);
	}

	public Object put(String key, Object value) {
		return attributes.put(key, value);
	}
	
}
