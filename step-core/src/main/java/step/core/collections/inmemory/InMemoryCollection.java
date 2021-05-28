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
package step.core.collections.inmemory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.collections.*;
import step.core.collections.PojoFilters.PojoFilterFactory;
import step.core.collections.filesystem.AbstractCollection;

public class InMemoryCollection<T> extends AbstractCollection<T> implements Collection<T> {

	private final Class<T> entityClass;
	private final Map<ObjectId, T> entities;
	private final ObjectMapper mapper = DefaultJacksonMapperProvider.getObjectMapper();
	
	public InMemoryCollection() {
		super();
		entityClass = null;
		entities = new ConcurrentHashMap<>();
	}
	
	public InMemoryCollection(Class<T> entityClass, Map<ObjectId, T> entities) {
		super();
		this.entityClass = entityClass;
		this.entities = entities;
	}

	@Override
	public List<String> distinct(String columnName, Filter filter) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Stream<T> find(Filter filter, SearchOrder order, Integer skip, Integer limit, int maxTime) {
		Stream<T> stream = filteredStream(filter);
		if(order != null) {
			Comparator<T> comparing = (Comparator<T>) PojoUtils.comparator(order.getAttributeName());
			if(order.getOrder()<0) {
				comparing = comparing.reversed();
			}
			stream = stream.sorted(comparing);
		}
		if(skip != null) {
			stream = stream.skip(skip);
		}
		if(limit != null) {
			stream = stream.limit(limit);
		}
		return stream.map(e -> {
			if(entityClass == Document.class && !(e instanceof Document)) {
				return (T) mapper.convertValue(e, Document.class);
			} else if(e instanceof Document && entityClass != Document.class) {
				return mapper.convertValue(e, entityClass);
			} else {
				return e;
			}
		});
	}

	@Override
	public Stream<T> findReduced(Filter filter, SearchOrder order, Integer skip, Integer limit, int maxTime, List<String> reduceFields) {
		return find(filter, order, skip, limit, maxTime);
	}

	private Stream<T> filteredStream(Filter filter) {
		PojoFilter<T> pojoFilter = new PojoFilterFactory<T>().buildFilter(filter);
		return entityStream().filter(pojoFilter::test).sorted(new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return getId(o1).compareTo(getId(o2));
			}
		});
	}

	private Stream<T> entityStream() {
		return entities.values().stream();
	}

	@Override
	public void remove(Filter filter) {
		filteredStream(filter).forEach(f->{
			entities.remove(getId(f));
		});
	}

	@Override
	public T save(T entity) {
		if (getId(entity) == null) {
			setId(entity, new ObjectId());
		}
		entities.put(getId(entity), entity);
		return entity;
	}

	@Override
	public void save(Iterable<T> entities) {
		if (entities != null) {
			entities.forEach(e -> save(e));
		}
	}

	@Override
	public void createOrUpdateIndex(String field) {
		
	}

	@Override
	public void createOrUpdateCompoundIndex(String... fields) {
		
	}

	@Override
	public void rename(String newName) {
		// TODO Auto-generated method stub
	}

	@Override
	public void drop() {
		// TODO Auto-generated method stub
		
	}
}
