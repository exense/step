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

import java.util.Iterator;
import java.util.Map;

import javax.json.JsonObjectBuilder;
import javax.json.spi.JsonProvider;

import org.bson.types.ObjectId;
import org.jongo.MongoCollection;

import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.MongoClientSession;



public class ArtefactAccessorImpl extends AbstractCRUDAccessor<AbstractArtefact> implements ArtefactAccessor {
			
	private MongoCollection artefacts;
		
	public ArtefactAccessorImpl(MongoClientSession clientSession) {
		super(clientSession, "artefacts", AbstractArtefact.class);
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
	
	private static final JsonProvider jsonProvider = JsonProvider.provider();
	
	@Override
	public AbstractArtefact findByAttributes(Map<String, String> attributes) {
		return findByAttributes(attributes, false);
	}
	
	@Override
	public AbstractArtefact findRootArtefactByAttributes(Map<String, String> attributes) {
		return findByAttributes(attributes, true);
	}
	
	protected AbstractArtefact findByAttributes(Map<String, String> attributes, boolean rootOnly) {
		JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
		for(String key:attributes.keySet()) {
			builder.add("attributes."+key, attributes.get(key));
		}
		if(rootOnly) {
			builder.add("root", true);			
		}

		String query = builder.build().toString();
		return artefacts.findOne(query).as(AbstractArtefact.class);
	}
	
	@Override
	public AbstractArtefact get(String artefactID) {
		return get(new ObjectId(artefactID));
	}
	
	@Override
	public Iterator<AbstractArtefact> getRootArtefacts() {
		JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
		builder.add("root", true);
		return artefacts.find(builder.build().toString()).as(AbstractArtefact.class);
	}
	
    @Override
	public Iterator<AbstractArtefact> getChildren(AbstractArtefact parent) {
    	final Iterator<ObjectId> childrenIdIt = parent.getChildrenIDs()!=null?parent.getChildrenIDs().iterator():null;
    	Iterator<AbstractArtefact> childrenIt = new Iterator<AbstractArtefact>() {
			@Override
			public void remove() {}
			
			@Override
			public AbstractArtefact next() {
				return get(childrenIdIt.next());
			}
			
			@Override
			public boolean hasNext() {
				return childrenIdIt!=null?childrenIdIt.hasNext():false;
			}
		};
    	return childrenIt;
    }
}
