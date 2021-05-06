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
package step.core;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractContext implements Closeable {
	
	private final ConcurrentHashMap<String, Object> attributes;
	private static final Logger logger = LoggerFactory.getLogger(AbstractContext.class);

	public AbstractContext() {
		super();
		this.attributes = new ConcurrentHashMap<String, Object>();
	}

	public Object get(Object key) {
		return attributes.get(key);
	}
	
	@SuppressWarnings("unchecked")
	public <T>T get(Class<T> class_) {
		return (T) get(key(class_));
	}
	
	@SuppressWarnings("unchecked")
	public <T>T require(Class<T> class_) {
		Object object = get(key(class_));
		if(object == null) {
			throw new DependencyException("Missing required dependency to "+class_.toString());
		} else {
			return (T) object;
		}
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
	
	@SuppressWarnings("unchecked")
	public <T> T put(Class<T> class_, T value) {
		return (T) attributes.put(key(class_), value);
	}

	private <T> String key(Class<T> class_) {
		return class_.getName();
	}
	
	public <T> T inheritFromParentOrComputeIfAbsent(AbstractContext parentContext, Class<T> class_,
			Function<Class<T>, T> mappingFunction) {
		T parentAttribute = parentContext != null ? parentContext.get(class_) : null;
		T value;
		if (parentAttribute == null) {
			value = mappingFunction.apply(class_);
		} else {
			value = parentAttribute;
		}
		put(class_, value);
		return value;
	}

	@Override
	public void close() throws IOException {
		attributes.values().forEach(v -> {
			if (v instanceof Closeable) {
				try {
					((Closeable) v).close();
				} catch (IOException e) {
					logger.error("Error while closing sesison object", e);
				}
			}
		});
	}
}
