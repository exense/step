package step.core.imports;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;

import step.core.GlobalContext;
import step.core.Version;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.CRUDAccessor;
import step.core.entities.Entity;

public class GenericDBImporter<A extends AbstractIdentifiableObject, T extends CRUDAccessor<A>> implements Importer<A, T> {
	
	protected Entity<A, T> entity;
	protected GlobalContext context;
	protected MongoCollection<Document> tmpCollection = null;
	
	public GenericDBImporter(GlobalContext context) {
		super();
		this.context = context;
	}

	public void init(Entity<A, T> entity) {
		this.entity = entity;
		if (entity.getAccessor() == null) {
			throw new IllegalArgumentException("No accessor mapped for entity type: " + entity.getName());
		}
	}
	
	public A importOne(ImportConfiguration importConfig, JsonParser jParser, ObjectMapper mapper,
			Map<String, String> references) throws JsonParseException, JsonMappingException, IOException {
		if (importConfig.version.compareTo(new Version(3,13,0)) >= 0) {
			A aObj = mapper.readValue(jParser, entity.getEntityClass());
			importConfig.objectEnricher.accept(aObj);
			if (importConfig.overwrite) {
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
			tmpCollection = context.getMongoClientSession().getMongoDatabase().getCollection(collectionName);
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
