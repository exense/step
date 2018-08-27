package step.plugins.views;

import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.MongoClientSession;

public class ViewModelAccessor extends AbstractCRUDAccessor<ViewModel> {

	public ViewModelAccessor(MongoClientSession clientSession) {
		super(clientSession, "views", ViewModel.class);
		createOrUpdateIndex(getMongoCollection("views"), "executionId");
	}
	
	public <T extends ViewModel> T get(String viewId, String executionId, Class<T> as) {
		return collection.findOne("{viewId:'"+viewId+"',executionId:'"+executionId+"'}").as(as);
	}
}
