/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.core.plugins;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginManager implements InvocationHandler{
	
	private static Logger logger = LoggerFactory.getLogger(PluginManager.class);
	
	private List<AbstractPlugin> plugins = new CopyOnWriteArrayList<>();
	
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
		plugins.add(plugin);
	}

	private AbstractPlugin newPluginInstance(Class<AbstractPlugin> _class) throws InstantiationException, IllegalAccessException  {
		AbstractPlugin plugin = _class.newInstance();
		return plugin;
	}

	@Override
	public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		for(AbstractPlugin plugin:plugins) {
			try {
				method.invoke(plugin, args);
			} catch (Throwable e) {
				logger.error("Error invoking method #" + method.getName() + " of plugin '" + plugin.getClass().getName() + "'" + "(" + e.toString() + ")", e);
			}
		}
		return null;
	}
}
