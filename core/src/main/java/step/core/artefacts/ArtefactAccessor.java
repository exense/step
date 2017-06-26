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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.json.JsonObjectBuilder;
import javax.json.spi.JsonProvider;

import org.bson.types.ObjectId;
import org.jongo.MongoCollection;

import com.mongodb.MongoClient;

import step.core.accessors.AbstractAccessor;
import step.core.accessors.MongoClientSession;
import step.core.accessors.MongoDBAccessorHelper;



public class ArtefactAccessor extends AbstractAccessor {
			
	private MongoCollection artefacts;
		
	@Deprecated
	public ArtefactAccessor() {
		super();
	}

	@Deprecated
	public ArtefactAccessor(MongoClient client) {
		super();
		artefacts = MongoDBAccessorHelper.getCollection(client, "artefacts");
	}
	
	public ArtefactAccessor(MongoClientSession clientSession) {
		super(clientSession);
		artefacts = getJongoCollection("artefacts");
	}
	
	public <T extends AbstractArtefact> T  createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name) {
		return createWorkArtefact(artefactClass, parentArtefact, name, false);
	}

	public <T extends AbstractArtefact> T  createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name, boolean copyChildren) {
		try {
			T artefact = artefactClass.newInstance();
			if(copyChildren) {
				for(ObjectId childId:parentArtefact.getChildrenIDs()) {
					artefact.addChild(childId);
				}
			}
			HashMap<String, String> attributes = new HashMap<>();
			attributes.put("name", name);
			artefact.setAttributes(attributes);
			return artefact;
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public AbstractArtefact get(ObjectId artefactID) {
		AbstractArtefact artefact = artefacts.findOne(artefactID).as(AbstractArtefact.class);
		return artefact;
	}
	
	private static final JsonProvider jsonProvider = JsonProvider.provider();
	
	public AbstractArtefact findByAttributes(Map<String, String> attributes) {
		JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
		for(String key:attributes.keySet()) {
			builder.add("attributes."+key, attributes.get(key));
		}

		String query = builder.build().toString();
		return artefacts.findOne(query).as(AbstractArtefact.class);
	}
	
	public Iterator<AbstractArtefact> getAll() {
		return artefacts.find().as(AbstractArtefact.class);
	}
	
	public void remove(ObjectId artefactID) {
		artefacts.remove(artefactID);
	}
	
	public AbstractArtefact get(String artefactID) {
		AbstractArtefact artefact = artefacts.findOne(new ObjectId(artefactID)).as(AbstractArtefact.class);
		return artefact;
	}
	
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
	
	public AbstractArtefact save(AbstractArtefact artefact) {
		artefacts.save(artefact);
		return artefact;
	}
	
	public void save(List<? extends AbstractArtefact> artefacts) {
		this.artefacts.insert(artefacts.toArray());
	}
}
