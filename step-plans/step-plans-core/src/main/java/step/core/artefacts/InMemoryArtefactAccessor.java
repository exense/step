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
package step.core.artefacts;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;

import step.core.accessors.InMemoryCRUDAccessor;

public class InMemoryArtefactAccessor extends InMemoryCRUDAccessor<AbstractArtefact> implements ArtefactAccessor {

	@Override
	public Iterator<AbstractArtefact> getChildren(AbstractArtefact parent) {
		return parent.getChildrenIDs().stream().map(id->get(id)).iterator();
	}
	
	public Collection<? extends AbstractArtefact> getCollection() {
		return map.values();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<AbstractArtefact> getRootArtefacts() {
		return (Iterator<AbstractArtefact>) getCollection().stream().filter(a->a.isRoot()).collect(Collectors.toList()).iterator();
	}

	private WorkArtefactFactory workArtefactFactory = new WorkArtefactFactory();
	
	@Override
	public <T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name) {
		return workArtefactFactory.createWorkArtefact(artefactClass, parentArtefact, name, false);
	}

	@Override
	public <T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name, boolean copyChildren) {
		return workArtefactFactory.createWorkArtefact(artefactClass, parentArtefact, name, copyChildren);
	}

	@Override
	public AbstractArtefact findRootArtefactByAttributes(Map<String, String> attributes) {
		return null;
	}

	@Override
	public AbstractArtefact get(String artefactID) {
		return get(new ObjectId(artefactID));
	}
}
