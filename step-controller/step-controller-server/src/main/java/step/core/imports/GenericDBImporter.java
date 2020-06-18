package step.core.imports;

import java.io.File;
import java.io.IOException;
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
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.CRUDAccessor;
import step.core.entities.Entity;
import step.core.objectenricher.ObjectEnricher;

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
	
	public void importOne(JsonParser jParser, ObjectMapper mapper, ObjectEnricher objectEnricher, Version version) throws JsonParseException, JsonMappingException, IOException {
		if (version.compareTo(new Version(3,13,0)) >= 0) {
			A aObj = mapper.readValue(jParser, entity.getEntityClass());
			objectEnricher.accept(aObj);
			entity.getAccessor().save(aObj);
		} else {
			saveToTmpCollection(mapper.readValue(jParser, Document.class));			
		}
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
	public void importMany(File file, ObjectMapper mapper, ObjectEnricher objectEnricher, Version version)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

}
