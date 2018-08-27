package step.core.accessors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;

public class AbstractAccessor {

	protected MongoClientSession mongoClientSession;
	
	public AbstractAccessor() {
		super();
	}

	public AbstractAccessor(MongoClientSession mongoClientSession) {
		super();
		this.mongoClientSession = mongoClientSession;
	}

	protected org.jongo.MongoCollection getJongoCollection(String collectionName) {
		return mongoClientSession.getJongoCollection(collectionName);
	}
	
	protected MongoCollection<Document> getMongoCollection(String collectionName) {
		return mongoClientSession.getMongoDatabase().getCollection(collectionName);
	}
	
	public static void createOrUpdateIndex(MongoCollection<Document> collection, String attribute) {
		Document index = getIndex(collection, attribute);
		if(index==null) {
			collection.createIndex(new Document(attribute,1));
		}
	}

	public static void createOrUpdateCompoundIndex(MongoCollection<Document> collection, String... attribute) {
		Document index = getIndex(collection, attribute);
		
		if(index==null) {
			Document newIndex = new Document();
			
			for(String s : attribute)
				newIndex.append(s, 1);

			collection.createIndex(newIndex);
		}
	}

	protected void createOrUpdateTTLIndex(MongoCollection<Document> collection, String attribute, Long ttl) {
		Document ttlIndex = getIndex(collection, attribute);
		if(ttlIndex==null) {
			if(ttl!=null && ttl>0) {
				createTimestampIndexWithTTL(collection, attribute, ttl);
			} else {
				createTimestampIndex(collection, attribute);
			}
		} else {
			if(ttl!=null && ttl>0) {
				if(!ttlIndex.containsKey("expireAfterSeconds") || !ttlIndex.getLong("expireAfterSeconds").equals(ttl)) {
					dropIndex(collection, ttlIndex);
					createTimestampIndexWithTTL(collection, attribute, ttl);
				}
			} else {
				if(ttlIndex.containsKey("expireAfterSeconds")) {
					dropIndex(collection, ttlIndex);
					createTimestampIndex(collection, attribute);
				}
			}
		}
	}

	private void dropIndex(MongoCollection<Document> collection, Document ttlIndex) {
		collection.dropIndex(ttlIndex.getString("name"));
	}

	private void createTimestampIndexWithTTL(MongoCollection<Document> collection, String attribute, Long ttl) {
		IndexOptions options = new IndexOptions();
		options.expireAfter(ttl, TimeUnit.SECONDS);
		createTimestampIndexWithOptions(collection, attribute, options);
	}

	private void createTimestampIndex(MongoCollection<Document> collection, String attribute) {
		IndexOptions options = new IndexOptions();
		createTimestampIndexWithOptions(collection, attribute, options);
	}

	private void createTimestampIndexWithOptions(MongoCollection<Document> collection, String attribute, IndexOptions options) {
		collection.createIndex(new Document(attribute, 1), options);
	}

	private static Document getIndex(MongoCollection<Document> collection, String... attribute) {
		HashSet<String> attributes = new HashSet<>(Arrays.asList(attribute));

		for(Document index:collection.listIndexes()) {  // inspect all indexes, looking for a match
			Object o = index.get("key");

			if(o instanceof Document) {
				Document d = ((Document)o);
				
				if(attributes.equals(d.keySet())) {
					return d;
				}
			}
		}
		return null;
	}
}
