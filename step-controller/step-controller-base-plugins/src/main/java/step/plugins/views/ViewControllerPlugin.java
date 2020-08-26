package step.plugins.views;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.ExecutionEnginePlugin;

@Plugin
public class ViewControllerPlugin extends AbstractControllerPlugin {
	
	private ViewManager viewManager;

	@Override
	public void executionControllerStart(GlobalContext context) {
		ViewModelAccessor accessor = new ViewModelAccessorImpl(context.getMongoClientSession());
		viewManager = new ViewManager(accessor);
		context.put(ViewModelAccessor.class, accessor);
		context.put(ViewManager.class, viewManager);

		context.getServiceRegistrationCallback().registerService(ViewPluginServices.class);
	}

	@Override
	public ExecutionEnginePlugin getExecutionEnginePlugin() {
		return new ViewPlugin(viewManager);
	}
}
