package step.resources;

import org.bson.Document;

import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.MongoClientSession;

public class ResourceAccessorImpl extends AbstractCRUDAccessor<Resource> implements ResourceAccessor {

	com.mongodb.client.MongoCollection<Document> resources_;
	
	public ResourceAccessorImpl(MongoClientSession clientSession) {
		super(clientSession, "resources", Resource.class);
		resources_ = getMongoCollection("resources");
		
		createIndexesIfNeeded();
	}
	
	protected void createIndexesIfNeeded() {
		createOrUpdateIndex(resources_, "resourceType");
	}

}
