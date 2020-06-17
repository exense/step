package step.core.imports;

import java.util.UUID;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;

import step.core.GlobalContext;
import step.core.accessors.CRUDAccessor;
import step.core.export.ImportExportMapper;

public abstract class GenericDBImporter {
	
	CRUDAccessor accessor;
	private GlobalContext context;
	private MongoCollection<Document> tmpCollection = null;

	public void init(GlobalContext c, String entityType) {
		this.accessor = ImportExportMapper.getAccessorByName(c, entityType);
		if (accessor == null) {
			throw new IllegalArgumentException("No accessor mapped for entity type: " + entityType);
		}
		this.context = c;
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

}
