package step.plugins.views;

import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.MongoClientSession;

public class ViewModelAccessorImpl extends AbstractCRUDAccessor<ViewModel> implements ViewModelAccessor {

	public ViewModelAccessorImpl(MongoClientSession clientSession) {
		super(clientSession, "views", ViewModel.class);
		createOrUpdateIndex(getMongoCollection("views"), "executionId");
	}
	
	@Override
	public <T extends ViewModel> T get(String viewId, String executionId, Class<T> as) {
		return collection.findOne("{viewId:'"+viewId+"',executionId:'"+executionId+"'}").as(as);
	}
	
	@Override
	public void removeViewsByExecutionId(String executionId) {
		collection.remove("{executionId:'"+executionId+"'}");
	}
}
