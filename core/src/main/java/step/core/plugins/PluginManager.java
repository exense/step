package step.core.plugins;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginManager implements InvocationHandler{
	
	private static Logger logger = LoggerFactory.getLogger(PluginManager.class);

	private List<AbstractPlugin> plugins = new ArrayList<>();
	
	public void initialize() throws Exception {
		loadAnnotatedPlugins();
	}
	
	public PluginCallbacks getProxy() {
		PluginCallbacks proxy = (PluginCallbacks) Proxy.newProxyInstance(
				PluginCallbacks.class.getClassLoader(),
				new Class[] { PluginCallbacks.class }, this);
		return proxy;
	}
	
	@SuppressWarnings("unchecked")
	private void loadAnnotatedPlugins() throws InstantiationException, IllegalAccessException  {
		Set<Class<?>> pluginClasses = new Reflections("step").getTypesAnnotatedWith(Plugin.class);
		
		for(Class<?> pluginClass:pluginClasses) {
			AbstractPlugin plugin = newPluginInstance((Class<AbstractPlugin>) pluginClass);
			register(plugin);
		}
	}

	public void register(AbstractPlugin plugin) {
		synchronized (plugins) {
			plugins.add(plugin);
		}
	}

	private AbstractPlugin newPluginInstance(Class<AbstractPlugin> _class) throws InstantiationException, IllegalAccessException  {
		AbstractPlugin plugin = _class.newInstance();
		return plugin;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		synchronized (plugins) {
			for(AbstractPlugin plugin:plugins) {
				try {
					method.invoke(plugin, args);
				} catch (Throwable e) {
					logger.error("Error invoking method #" + method.getName() + " of plugin '" + plugin.getClass().getName() + "'" + "(" + e.toString() + ")");
				}
			}
		}
		return null;
	}
}
