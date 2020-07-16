package step.core.plugins;

import java.util.ArrayList;
import java.util.List;

import ch.exense.commons.app.Configuration;
import step.core.plugins.PluginManager.Builder;
import step.core.plugins.PluginManager.Builder.CircularDependencyException;

public class ControllerPluginManager {
	
	protected Configuration configuration;
	
	protected PluginManager<ExecutionCallbacks> pluginManager;
	
	public ControllerPluginManager(Configuration configuration) throws CircularDependencyException, InstantiationException, IllegalAccessException {
		this.configuration = configuration;
		Builder<ExecutionCallbacks> builder = new PluginManager.Builder<ExecutionCallbacks>(ExecutionCallbacks.class);
		this.pluginManager = builder.withPluginsFromClasspath().withPluginFilter(p->isPluginEnabled(p)).build();
	}

	public ControllerPlugin getProxy() {
		return pluginManager.getProxy(ControllerPlugin.class);
	}

	public List<WebPlugin> getWebPlugins() {
		List<WebPlugin> webPlugins = new ArrayList<>();
		for (ExecutionCallbacks plugin : pluginManager.getPlugins()) {
			if(plugin instanceof ControllerPlugin) {
				ControllerPlugin controllerPlugin = (ControllerPlugin) plugin;
				WebPlugin webPlugin = controllerPlugin.getWebPlugin();
				if(webPlugin != null) {
					webPlugins.add(webPlugin);
				}
			}
		}
		return webPlugins;
	}

	private boolean isPluginEnabled(Object plugin) {
		return configuration.getPropertyAsBoolean("plugins."+plugin.getClass().getSimpleName()+".enabled", true);
	}
}
