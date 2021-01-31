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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.bson.types.ObjectId;

public class InMemoryCRUDAccessor<T extends AbstractIdentifiableObject> implements CRUDAccessor<T> {

	protected Map<ObjectId, T> map = new ConcurrentHashMap<>();

	@Override
	public T get(ObjectId id) {
		return map.get(id);
	}

	@Override
	public T get(String id) {
		return get(new ObjectId(id));
	}

	@Override
	public T findByAttributes(Map<String, String> attributes) {
		return findByAttributesStream(attributes).findFirst().orElse(null);
	}

	@Override
	public Spliterator<T> findManyByAttributes(Map<String, String> attributes) {
		return findByAttributesStream(attributes).spliterator();
	}

	@Override
	public T findByAttributes(Map<String, String> attributes, String attributesMapKey) {
		return findByAttributesStream(attributes, attributesMapKey).findFirst().orElse(null);
	}

	@Override
	public Spliterator<T> findManyByAttributes(Map<String, String> attributes, String attributesMapKey) {
		return findByAttributesStream(attributes, attributesMapKey).spliterator();
	}

	private Stream<T> findByAttributesStream(Map<String, String> attributes) {
		return map.values().stream().filter(v -> {
			if (v instanceof AbstractOrganizableObject) {
				return areAllEntriesContainedInMap(((AbstractOrganizableObject) v).attributes, attributes);
			} else if (v instanceof AbstractIdentifiableObject) {
				return areAllEntriesContainedInMap(((AbstractIdentifiableObject) v).customFields, attributes);
			} else {
				return false;
			}
		});
	}
	
	private Stream<T> findByAttributesStream(Map<String, String> attributes, String attributesMapKey) {
		return map.values().stream().filter(v -> {
			if (attributesMapKey.equals("attributes")) {
				return areAllEntriesContainedInMap(((AbstractOrganizableObject) v).attributes, attributes);
			} else if (attributesMapKey.equals("customFields")) {
				return areAllEntriesContainedInMap(((AbstractIdentifiableObject) v).customFields, attributes);
			} else {
				throw new IllegalArgumentException("Unknown field "+attributesMapKey);
			}
		});
	}
	
	private boolean areAllEntriesContainedInMap(Map<?, ?> map, Map<?, ?> entries) {
		if(map != null) {
			if(entries != null) {
				return map.entrySet().containsAll(entries.entrySet());
			} else {
				return true;
			}
		} else {
			return entries == null || entries.isEmpty();
		}
	}

	@Override
	public Iterator<T> getAll() {
		return map.values().iterator();
	}

	@Override
	public void remove(ObjectId id) {
		map.remove(id);
	}

	@Override
	public T save(T entity) {
		if(entity.getId()==null) {
			entity.setId(new ObjectId());
		}
		map.put(entity.getId(), entity);
		return entity;
	}

	@Override
	public void save(Collection<? extends T> entities) {
		if(entities != null && entities.size()>0) {
			entities.forEach(e->save(e));
		}
	}

	@Override
	public List<T> getRange(int skip, int limit) {
		List<T> list = new ArrayList<>(map.values());
		if(skip<list.size()) {
			return list.subList(skip, Math.min(list.size(), skip+limit));
		} else {
			return new ArrayList<>();
		}
	}
	
	protected RuntimeException notImplemented(){
		return new RuntimeException("Not implemented");
	}

}
