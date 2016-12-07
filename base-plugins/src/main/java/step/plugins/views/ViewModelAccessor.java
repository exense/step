package step.plugins.views;

import org.jongo.MongoCollection;

import com.mongodb.MongoClient;

import step.core.accessors.MongoDBAccessorHelper;

public class ViewModelAccessor {

	private MongoCollection collection;
	
	public ViewModelAccessor(MongoClient mongoClient) {
		super();		
		collection = MongoDBAccessorHelper.getCollection(mongoClient, "views");
	}

	public <T extends ViewModel>  void save(T instance) {
		collection.save(instance);
	}
	
	public <T extends ViewModel> T get(String viewId, String executionId, Class<T> as) {
		return collection.findOne("{viewId:'"+viewId+"',executionId:'"+executionId+"'}").as(as);
	}
}
