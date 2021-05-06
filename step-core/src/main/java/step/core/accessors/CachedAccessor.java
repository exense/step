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
package step.core.accessors;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;

import org.bson.types.ObjectId;

/**
 * This {@link Accessor} loads all the entities of the provided underlying
 * {@link Accessor} at initialization and keeps them in memory for further
 * accesses. Write operations like remove and save are persisted in the
 * underlying {@link Accessor}
 *
 * @param <T> the type of the entity
 */
public class CachedAccessor<T extends AbstractIdentifiableObject> implements Accessor<T> {

	private final Accessor<T> cache = new InMemoryAccessor<T>();

	private final Accessor<T> underlyingAccessor;

	/**
	 * @param underlyingAccessor the {@link Accessor} from which the entities should
	 *                           be loaded
	 */
	public CachedAccessor(Accessor<T> underlyingAccessor) {
		super();
		this.underlyingAccessor = underlyingAccessor;
		reloadCache();
	}

	/**
	 * Reloads all the entities from the underlying {@link Accessor}
	 */
	public void reloadCache() {
		// Load cache
		underlyingAccessor.getAll().forEachRemaining(e -> cache.save(e));
	}

	@Override
	public T get(ObjectId id) {
		return cache.get(id);
	}

	@Override
	public T get(String id) {
		return cache.get(id);
	}

	@Override
	public T findByAttributes(Map<String, String> attributes) {
		return cache.findByAttributes(attributes);
	}

	@Override
	public Spliterator<T> findManyByAttributes(Map<String, String> attributes) {
		return cache.findManyByAttributes(attributes);
	}

	@Override
	public T findByAttributes(Map<String, String> attributes, String attributesMapKey) {
		return cache.findByAttributes(attributes, attributesMapKey);
	}

	@Override
	public Spliterator<T> findManyByAttributes(Map<String, String> attributes, String attributesMapKey) {
		return cache.findManyByAttributes(attributes, attributesMapKey);
	}

	@Override
	public Iterator<T> getAll() {
		return cache.getAll();
	}

	@Override
	public void remove(ObjectId id) {
		cache.remove(id);
		underlyingAccessor.remove(id);
	}

	@Override
	public T save(T entity) {
		T result = underlyingAccessor.save(entity);
		cache.save(result);
		return result;
	}

	@Override
	public void save(Iterable<T> entities) {
		entities.forEach(e -> save(e));
	}

	@Override
	public List<T> getRange(int skip, int limit) {
		return cache.getRange(skip, limit);
	}
}
