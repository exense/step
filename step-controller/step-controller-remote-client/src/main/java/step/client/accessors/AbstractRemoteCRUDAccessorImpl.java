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
package step.client.accessors;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.bson.types.ObjectId;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.commons.iterators.SkipLimitIterator;
import step.commons.iterators.SkipLimitProvider;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.CRUDAccessor;

public class AbstractRemoteCRUDAccessorImpl<T extends AbstractIdentifiableObject> extends AbstractRemoteClient implements CRUDAccessor<T>{

	protected final String path;
	protected final Class<T> entityClass;
	
	public AbstractRemoteCRUDAccessorImpl(String path, Class<T> entityClass) {
		super();
		this.path = path;
		this.entityClass = entityClass;
	}
	
	public AbstractRemoteCRUDAccessorImpl(ControllerCredentials credentials, String path, Class<T> entityClass) {
		super(credentials);
		this.path = path;
		this.entityClass = entityClass;
	}

	@Override
	public T get(ObjectId id) {
		Builder b = requestBuilder(path+id.toString());
		return executeRequest(()->b.get(entityClass));
	}

	@Override
	public T get(String id) {
		return get(new ObjectId(id));
	}

	@Override
	public T findByAttributes(Map<String, String> attributes) {
		Builder b = requestBuilder(path+"search");
		Entity<Map<String, String>> entity = Entity.entity(attributes, MediaType.APPLICATION_JSON);
		return  executeRequest(()->b.post(entity,entityClass));
	}

	@Override
	public Spliterator<T> findManyByAttributes(Map<String, String> attributes) {
		Builder b = requestBuilder(path+"find");
		Entity<Map<String, String>> entity = Entity.entity(attributes, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity, genericEntity)).spliterator();
	}

	@Override
	public Iterator<T> getAll() {
		return new SkipLimitIterator<T>(new SkipLimitProvider<T>() {
			@Override
			public List<T> getBatch(int skip, int limit) {
				return getRange(skip, limit);
			}
		});
	}
	
	protected ParameterizedType parameterizedGenericType = new ParameterizedType() {
        public Type[] getActualTypeArguments() {
            return new Type[] { entityClass };
        }

        public Type getRawType() {
            return List.class;
        }

        public Type getOwnerType() {
            return List.class;
        }
    };
    
    protected GenericType<List<T>> genericEntity = new GenericType<List<T>>(
			parameterizedGenericType) {
    };
	
	@Override
	public List<T> getRange(int skip, int limit) {
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("skip", Integer.toString(skip));
		queryParams.put("limit", Integer.toString(limit));
		GenericType<List<T>> genericEntity = new GenericType<List<T>>(
				parameterizedGenericType) {
	    };
		Builder b = requestBuilder(path, queryParams);
		return executeRequest(()->b.get(genericEntity));
	}

	@Override
	public T findByAttributes(Map<String, String> attributes, String attributesMapKey) {
		throw notImplemented();
	}

	@Override
	public Spliterator<T> findManyByAttributes(Map<String, String> attributes, String attributesMapKey) {
		throw notImplemented();
	}

	@Override
	public void remove(ObjectId id) {
		Builder b = requestBuilder(path+id);
		executeRequest(()->b.delete(entityClass));
	}

	@Override
	public T save(T e) {
		Builder b = requestBuilder(path);
		Entity<?> entity = Entity.entity(e, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity, entityClass));
	}

	@Override
	public void save(Collection<? extends T> entities) {
		entities.forEach(e->save(e));
	}
}
