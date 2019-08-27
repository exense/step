package step.core.plugins;

import java.util.ArrayList;
import java.util.List;

import ch.exense.commons.app.Configuration;

public class ControllerPluginManager extends PluginManager<AbstractControllerPlugin> {
	
	protected Configuration configuration;
	
	public ControllerPluginManager(Configuration configuration) {
		super();
		this.configuration = configuration;
	}
	
	public ControllerPluginCallbacks getProxy() {
		return super.getProxy(ControllerPluginCallbacks.class);
	}


	public List<WebPlugin> getWebPlugins() {
		List<WebPlugin> webPlugins = new ArrayList<>();
		for(AbstractControllerPlugin plugin:plugins) {
			WebPlugin webPlugin = plugin.getWebPlugin();
			if(webPlugin!=null) {
				webPlugins.add(webPlugin);
			}
		}
		return webPlugins;
	}

	@Override
	public void register(AbstractControllerPlugin plugin) {
		boolean isPluginEnabled = configuration.getPropertyAsBoolean("plugins."+plugin.getClass().getSimpleName()+".enabled", true);
		if(isPluginEnabled) {
			super.register(plugin);
		}
	}
}
