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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.types.ObjectId;

import ch.exense.commons.app.Configuration;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;

public class InMemoryCollectionFactory implements CollectionFactory {

	private final Map<String, Map<ObjectId, Object>> collections = new ConcurrentHashMap<>();
	
	public InMemoryCollectionFactory(Configuration configuration) {
		super();
	}

	@Override
	public void close() throws IOException {
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Collection<T> getCollection(String name, Class<T> entityClass) {
		Map<ObjectId, Object> entities = collections.computeIfAbsent(name, k->new ConcurrentHashMap<ObjectId, Object>());
		return (Collection<T>) new InMemoryCollection<T>(entityClass, (Map<ObjectId, T>) entities);
	}

}
