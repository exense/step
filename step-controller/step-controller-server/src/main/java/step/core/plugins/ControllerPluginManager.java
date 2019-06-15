package step.core.plugins;

import java.util.ArrayList;
import java.util.List;

public class ControllerPluginManager extends PluginManager<AbstractControllerPlugin> {
	
	public ControllerPluginManager() {
		super();
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
}
