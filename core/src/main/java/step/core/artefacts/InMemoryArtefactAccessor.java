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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

public class InMemoryArtefactAccessor extends ArtefactAccessor {

	Map<ObjectId, AbstractArtefact> map = new HashMap<>();

	@Override
	public AbstractArtefact get(ObjectId artefactID) {
		return map.get(artefactID);
	}

	@Override
	public AbstractArtefact get(String artefactID) {
		return get(new ObjectId(artefactID));
	}

	@Override
	public Iterator<AbstractArtefact> getChildren(AbstractArtefact parent) {
		return parent.getChildrenIDs().stream().map(id->get(id)).iterator();
	}

	@Override
	public AbstractArtefact save(AbstractArtefact artefact) {
		return map.put(artefact.getId(), artefact);
	}

	@Override
	public void save(List<? extends AbstractArtefact> artefacts) {
		artefacts.stream().forEach(artefact->save(artefact));
	}
	
	public Collection<? extends AbstractArtefact> getCollection() {
		return map.values();
	}

}
