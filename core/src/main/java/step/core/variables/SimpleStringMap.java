package step.core.variables;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class SimpleStringMap implements Map<String, String> {

	@Override
	public int size() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean isEmpty() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean containsKey(Object key) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean containsValue(Object value) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public String get(Object key) {
		return get((String)key);
	}
	
	public abstract String get(String key);

	@Override
	public abstract String put(String key, String value);

	@Override
	public String remove(Object key) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void putAll(Map<? extends String, ? extends String> m) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void clear() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Set<String> keySet() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Collection<String> values() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		throw new RuntimeException("Not implemented");
	}

}
