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
package step.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public abstract class AbstractContext {
	
	private final ConcurrentHashMap<String, Object> attributes;

	public AbstractContext() {
		this(null);
	}

	public AbstractContext(AbstractContext parentContext) {
		super();
		this.attributes = new ConcurrentHashMap<String, Object>();
		if(parentContext != null) {
			this.attributes.putAll(parentContext.attributes);
		}
	}

	public Object get(Object key) {
		return attributes.get(key);
	}
	
	@SuppressWarnings("unchecked")
	public <T>T get(Class<T> class_) {
		return (T) get(key(class_));
	}
	
	public <T> T computeIfAbsent(Class<T> class_, Function<Class<T>, T> mappingFunction) {
		T value = get(class_);
		if(value == null) {
			value = mappingFunction.apply(class_);
			put(class_, value);
		}
		return value;
	}

	public Object put(String key, Object value) {
		return attributes.put(key, value);
	}
	
	public <T> Object put(Class<T> class_, T value) {
		return attributes.put(key(class_), value);
	}

	private <T> String key(Class<T> class_) {
		return class_.getName();
	}
}
