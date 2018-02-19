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
package step.core.variables;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class SimpleStringMap implements Map<String, String> {

	@Override
	public abstract int size();

	@Override
	public abstract boolean isEmpty();

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
		for(String key:m.keySet()) {
			put(key, m.get(key));
		}
	}

	@Override
	public void clear() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public abstract Set<String> keySet();

	@Override
	public Collection<String> values() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public String toString() {
		return keySet().stream().map(key->key+"="+get(key)).collect(Collectors.joining(" "));
	}

}
