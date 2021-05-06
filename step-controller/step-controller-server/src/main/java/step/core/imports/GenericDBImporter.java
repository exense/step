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
package step.core.imports;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import com.mongodb.BasicDBObject;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;

import org.jongo.Mapper;
import org.jongo.marshall.Unmarshaller;
import org.jongo.marshall.jackson.JacksonMapper;
import step.core.GlobalContext;
import step.core.Version;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AccessorLayerJacksonMapperProvider;
import step.core.collections.mongodb.MongoClientSession;
import step.core.accessors.Accessor;
import step.core.deployment.JacksonMapperProvider;
import step.core.entities.Entity;

public class GenericDBImporter<A extends AbstractIdentifiableObject, T extends Accessor<A>> implements Importer<A, T> {
	
	protected Entity<A, T> entity;
	protected GlobalContext context;
	protected MongoCollection<Document> tmpCollection = null;
	private Mapper dbLayerObjectMapper;
	private Unmarshaller unmarshaller;
	
	public GenericDBImporter(GlobalContext context) {
		super();
		this.context = context;
		JacksonMapper.Builder builder2 = new JacksonMapper.Builder();
		AccessorLayerJacksonMapperProvider.getModules().forEach(m->builder2.registerModule(m));
		JacksonMapperProvider.getModules().forEach(m->builder2.registerModule(m));
		dbLayerObjectMapper = builder2.build();
		unmarshaller = dbLayerObjectMapper.getUnmarshaller();
	}

	public void init(Entity<A, T> entity) {
		this.entity = entity;
		if (entity.getAccessor() == null) {
			throw new IllegalArgumentException("No accessor mapped for entity type: " + entity.getName());
		}
	}
	
	public A importOne(ImportConfiguration importConfig, JsonParser jParser, ObjectMapper mapper,
			Map<String, String> references) throws JsonParseException, JsonMappingException, IOException {
		if (importConfig.getVersion().compareTo(new Version(3,13,0)) >= 0) {
			//A aObj = mapper.readValue(jParser, entity.getEntityClass());
			BasicDBObject o = mapper.readValue(jParser, BasicDBObject.class);
			applyMigrationTasks(importConfig, o);
			A aObj = unmarshaller.unmarshall(org.jongo.bson.Bson.createDocument(o),entity.getEntityClass());
			context.getEntityManager().runImportHooks(aObj, importConfig);
			if (importConfig.isOverwrite()) {
				entity.getAccessor().save(aObj);
			} else {
				saveWithNewId(aObj,references);
			}
			return aObj;
			
		} else {
			saveToTmpCollection(mapper.readValue(jParser, Document.class));
			return null;
		}
	}

	protected BasicDBObject applyMigrationTasks(ImportConfiguration importConfig, BasicDBObject o) {
		return o;
	}

	protected void saveWithNewId(A aObj, Map<String, String> references) {
		String origId = aObj.getId().toHexString();
		ObjectId objectId;
		//if the origId was already replaced, use the new one
		if (references.containsKey(origId)) {
			objectId = new ObjectId(references.get(origId));
		} else {
			objectId = new ObjectId();
		}
		aObj.setId(objectId);
		references.put(origId, aObj.getId().toHexString());
		context.getEntityManager().updateReferences(aObj, references);
		entity.getAccessor().save(aObj);
	}
	
	protected MongoCollection<Document> getOrInitTmpCollection() {
		if (tmpCollection == null) {
			String collectionName = "Tmp" + UUID.randomUUID();
			tmpCollection = context.require(MongoClientSession.class).getMongoDatabase().getCollection(collectionName);
		} 
		return tmpCollection;
	}
	
	protected MongoCollection<Document> getTmpCollection() {
		return tmpCollection;
	}
	
	protected void saveToTmpCollection(Document doc) {
		if (doc.containsKey("id") && !doc.containsKey("_id") ) {
			String id = (String )doc.remove("id");
			doc.put("_id", new ObjectId(id));
			
		}
		getOrInitTmpCollection().insertOne(doc);	
	}

	@Override
	public void importMany(ImportConfiguration importConfig, ObjectMapper mapper)
			throws IOException {
		throw new RuntimeException("Not implemented");
	}


}
