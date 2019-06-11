package step.plugins.views;

import step.core.accessors.CRUDAccessor;

public interface ViewModelAccessor extends CRUDAccessor<ViewModel> {

	<T extends ViewModel> T get(String viewId, String executionId, Class<T> as);

	void removeViewsByExecutionId(String executionId);

}