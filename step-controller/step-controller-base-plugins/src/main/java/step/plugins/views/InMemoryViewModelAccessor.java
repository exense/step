package step.plugins.views;

import step.core.accessors.InMemoryCRUDAccessor;

public class InMemoryViewModelAccessor extends InMemoryCRUDAccessor<ViewModel> implements ViewModelAccessor {

	@Override
	public <T extends ViewModel> T get(String viewId, String executionId, Class<T> as) {
		return null;
	}

	@Override
	public void removeViewsByExecutionId(String executionId) {

	}

}
