package step.plugins.views;

import org.jongo.MongoCollection;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import step.core.accessors.AbstractAccessor;
import step.core.accessors.MongoDBAccessorHelper;

public class ViewModelAccessor extends AbstractAccessor {

	private MongoCollection collection;
	
	public ViewModelAccessor(MongoClient mongoClient, MongoDatabase mongoDatabase) {
		super();		
		collection = MongoDBAccessorHelper.getCollection(mongoClient, "views");
		
		createOrUpdateIndex(mongoDatabase.getCollection("views"), "executionId");
	}

	public <T extends ViewModel>  void save(T instance) {
		collection.save(instance);
	}
	
	public <T extends ViewModel> T get(String viewId, String executionId, Class<T> as) {
		return collection.findOne("{viewId:'"+viewId+"',executionId:'"+executionId+"'}").as(as);
	}
}
