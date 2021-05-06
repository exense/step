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

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.commons.beanutils.PropertyUtils;
import org.bson.types.ObjectId;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.PojoFilter;
import step.core.collections.PojoFilters.PojoFilterFactory;
import step.core.collections.SearchOrder;

public class InMemoryCollection<T extends AbstractIdentifiableObject> implements Collection<T> {

	private final Map<ObjectId, T> entities = new ConcurrentHashMap<>();
	
	public InMemoryCollection() {
		super();
	}

	@Override
	public List<String> distinct(String columnName, Filter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> distinct(String columnName) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Stream<T> find(Filter filter, SearchOrder order, Integer skip, Integer limit, int maxTime) {
		Stream<T> stream = filteredStream(filter);
		if(skip != null) {
			stream = stream.skip(skip);
		}
		if(limit != null) {
			stream = stream.limit(limit);
		}
		if(order != null) {
			Comparator<T> comparing = Comparator.comparing(e->{
				try {
					return PropertyUtils.getProperty(e, order.getAttributeName()).toString();
				} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
					throw new RuntimeException(e1);
				}
			});
			if(order.getOrder()<0) {
				comparing = comparing.reversed();
			}
			stream = stream.sorted(comparing);
		}
		return stream;
	}

	private Stream<T> filteredStream(Filter filter) {
		PojoFilter<T> pojoFilter = new PojoFilterFactory<T>().buildFilter(filter);
		return entityStream().filter(pojoFilter::test).sorted(new Comparator<T>() {
				@Override
				public int compare(T o1, T o2) {
					return o1.getId().compareTo(o2.getId());
				}
			});
	}

	private Stream<T> entityStream() {
		return entities.values().stream();
	}

	@Override
	public void remove(Filter filter) {
		filteredStream(filter).forEach(f->{
			entities.remove(f.getId());
		});
	}

	@Override
	public T save(T entity) {
		if (entity.getId() == null) {
			entity.setId(new ObjectId());
		}
		entities.put(entity.getId(), entity);
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
}
