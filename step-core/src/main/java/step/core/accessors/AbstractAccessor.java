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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.types.ObjectId;

import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.collections.Filters.Equals;

public class AbstractAccessor<T extends AbstractIdentifiableObject> implements Accessor<T> {

	protected final Collection<T> collectionDriver;

	public AbstractAccessor(Collection<T> collectionDriver) {
		super();
		this.collectionDriver = collectionDriver;
	}

	public Collection<T> getCollectionDriver() {
		return collectionDriver;
	}

	@Override
	public T get(ObjectId id) {
		return collectionDriver.find(byId(id), null, null, null, 0).findFirst().orElse(null);
	}

	@Override
	public T get(String id) {
		return get(new ObjectId(id));
	}

	@Override
	public T findByAttributes(Map<String, String> attributes) {
		return findByKeyAttributes("attributes", attributes);
	}

	@Override
	public Spliterator<T> findManyByAttributes(Map<String, String> attributes) {
		return findByAttributesStream("attributes", attributes).spliterator();
	}

	@Override
	public T findByAttributes(Map<String, String> attributes, String attributesMapKey) {
		return findByKeyAttributes(attributesMapKey, attributes);
	}

	@Override
	public Spliterator<T> findManyByAttributes(Map<String, String> attributes, String attributesMapKey) {
		return findByAttributesStream(attributesMapKey, attributes).spliterator();
	}

	private T findByKeyAttributes(String fieldName, Map<String, String> attributes) {
		Stream<T> stream = findByAttributesStream(fieldName, attributes);
		return stream.findFirst().orElse(null);
	}

	private Stream<T> findByAttributesStream(String fieldName, Map<String, String> attributes) {
		Filter filter;
		if(attributes != null) {
			filter = Filters.and(attributes.entrySet().stream()
					.map(e -> Filters.equals(fieldName + "." + e.getKey(), e.getValue())).collect(Collectors.toList()));
		} else {
			filter = Filters.empty();
		}
		return collectionDriver.find(filter, null, null, null, 0);
	}

	@Override
	public Iterator<T> getAll() {
		return collectionDriver.find(Filters.empty(), null, null, null, 0).iterator();
	}
	
	public Stream<T> stream() {
		return collectionDriver.find(Filters.empty(), null, null, null, 0);
	}

	@Override
	public void remove(ObjectId id) {
		collectionDriver.remove(byId(id));
	}

	private Equals byId(ObjectId id) {
		return Filters.equals("id", id);
	}

	@Override
	public T save(T entity) {
		return collectionDriver.save(entity);
	}

	@Override
	public void save(Iterable<T> entities) {
		collectionDriver.save((Iterable<T>)entities);
	}

	@Override
	public List<T> getRange(int skip, int limit) {
		return collectionDriver.find(Filters.empty(), null, skip, limit, 0).collect(Collectors.toList());
	}
	
	protected void createOrUpdateIndex(String field) {
		collectionDriver.createOrUpdateIndex(field);
	}
	
	protected void createOrUpdateCompoundIndex(String... fields) {
		collectionDriver.createOrUpdateCompoundIndex(fields);
	}
}
