/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.core.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DocumentObject implements Map<String, Object> {

	private final Map<String, Object> map;

	public DocumentObject() {
		this(new HashMap<>());
	}

	public DocumentObject(Map<String, Object> map) {
		super();
		this.map = map;
	}

	@SuppressWarnings("unchecked")
	public DocumentObject getObject(String key) {
		Object object = get(key);
		if (object != null) {
			if (object instanceof Map) {
				return new DocumentObject((Map<String, Object>) object);
			} else {
				throw new RuntimeException("Value of " + key + " is not a map but " + object.getClass().getName());
			}
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<DocumentObject> getArray(String key) {
		Object object = get(key);
		if (object != null) {
			if (object instanceof List) {
				return (List<DocumentObject>) ((List<?>) object).stream()
						.map(e -> new DocumentObject((Map<String, Object>) e)).collect(Collectors.toList());
			} else {
				throw new RuntimeException("Value of " + key + " is not a map but " + object.getClass().getName());
			}
		} else {
			return null;
		}
	}

	public String getString(String key) {
		Object object = get(key);
		return object != null ? object.toString() : null;
	}

	public boolean getBoolean(String key) {
		Object object = get(key);
		if (object != null) {
			if (object instanceof Boolean) {
				return (Boolean) object;
			} else if (object instanceof String) {
				return Boolean.parseBoolean((String) object);
			} else {
				throw new RuntimeException("Value " + object + " is not a boolean");
			}
		} else {
			return false;
		}

	}

	public int size() {
		return map.size();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	public Object get(Object key) {
		return map.get(key);
	}

	public Object put(String key, Object value) {
		return map.put(key, value);
	}

	public Object remove(Object key) {
		return map.remove(key);
	}

	public void putAll(Map<? extends String, ? extends Object> m) {
		map.putAll(m);
	}

	public void clear() {
		map.clear();
	}

	public Set<String> keySet() {
		return map.keySet();
	}

	public Collection<Object> values() {
		return map.values();
	}

	public Set<Entry<String, Object>> entrySet() {
		return map.entrySet();
	}

	public boolean equals(Object o) {
		return map.equals(o);
	}

	public int hashCode() {
		return map.hashCode();
	}

	public Object getOrDefault(Object key, Object defaultValue) {
		return map.getOrDefault(key, defaultValue);
	}

	public void forEach(BiConsumer<? super String, ? super Object> action) {
		map.forEach(action);
	}

	public void replaceAll(BiFunction<? super String, ? super Object, ? extends Object> function) {
		map.replaceAll(function);
	}

	public Object putIfAbsent(String key, Object value) {
		return map.putIfAbsent(key, value);
	}

	public boolean remove(Object key, Object value) {
		return map.remove(key, value);
	}

	public boolean replace(String key, Object oldValue, Object newValue) {
		return map.replace(key, oldValue, newValue);
	}

	public Object replace(String key, Object value) {
		return map.replace(key, value);
	}

	public Object computeIfAbsent(String key, Function<? super String, ? extends Object> mappingFunction) {
		return map.computeIfAbsent(key, mappingFunction);
	}

	public Object computeIfPresent(String key,
			BiFunction<? super String, ? super Object, ? extends Object> remappingFunction) {
		return map.computeIfPresent(key, remappingFunction);
	}

	public Object compute(String key, BiFunction<? super String, ? super Object, ? extends Object> remappingFunction) {
		return map.compute(key, remappingFunction);
	}

	public Object merge(String key, Object value,
			BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
		return map.merge(key, value, remappingFunction);
	}
}