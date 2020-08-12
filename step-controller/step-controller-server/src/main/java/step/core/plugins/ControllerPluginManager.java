package step.core.plugins;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import ch.exense.commons.app.Configuration;
import step.core.plugins.PluginManager.Builder;
import step.core.plugins.PluginManager.Builder.CircularDependencyException;
import step.engine.plugins.ExecutionEnginePlugin;

public class ControllerPluginManager {
	
	protected Configuration configuration;
	
	protected PluginManager<ControllerPlugin> pluginManager;
	
	public ControllerPluginManager(Configuration configuration) throws CircularDependencyException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		this.configuration = configuration;
		Builder<ControllerPlugin> builder = new PluginManager.Builder<ControllerPlugin>(ControllerPlugin.class);
		this.pluginManager = builder.withPluginsFromClasspath().withPluginFilter(this::isPluginEnabled).build();
	}

	public ControllerPlugin getProxy() {
		return pluginManager.getProxy(ControllerPlugin.class);
	}

	public List<ExecutionEnginePlugin> getExecutionEnginePlugins() {
		return pluginManager.getPlugins().stream().map(ControllerPlugin::getExecutionEnginePlugin).filter(Objects::nonNull).collect(Collectors.toList());
	}
	
	public List<WebPlugin> getWebPlugins() {
		return pluginManager.getPlugins().stream().map(ControllerPlugin::getWebPlugin).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private boolean isPluginEnabled(Object plugin) {
		return configuration.getPropertyAsBoolean("plugins."+plugin.getClass().getSimpleName()+".enabled", true);
	}
}
