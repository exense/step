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
package step.core.accessors;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

public class InMemoryAccessor<T extends AbstractDBObject> implements CRUDAccessor<T> {

	public InMemoryAccessor() {
	}

	Map<ObjectId, T> map = new HashMap<>();

	@Override
	public T get(ObjectId artefactID) {
		return map.get(artefactID);
	}

	@Override
	public T save(T entity) {
		return map.put(entity.getId(), entity);
	}

	@Override
	public void save(List<? extends T> artefacts) {
		artefacts.stream().forEach(artefact->save(artefact));
	}
	
	public Collection<? extends T> getCollection() {
		return map.values();
	}

	@Override
	public T findByAttributes(Map<String, String> attributes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<T> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(ObjectId id) {
		// TODO Auto-generated method stub
		
	}

}
