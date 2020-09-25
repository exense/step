/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.core.plugins;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.plugins.exceptions.PluginCriticalException;
import step.core.scanner.CachedAnnotationScanner;

public class PluginManager<T> {
	
	private static Logger logger = LoggerFactory.getLogger(PluginManager.class);
	
	private final Class<T> pluginClass;
	private final List<T> plugins;
	
	private PluginManager(Class<T> pluginClass, List<T> plugins) {
		super();
		this.pluginClass = pluginClass;
		this.plugins = plugins;
		
		logger.info("Starting plugin manager with following plugins: "+Arrays.toString(plugins.toArray()));
	}
	
	public T getProxy() {
		return getProxy(pluginClass);
	}

	public <I> I getProxy(Class<I> proxyInterface) {
		@SuppressWarnings("unchecked")
		I proxy = (I) Proxy.newProxyInstance(
				proxyInterface.getClassLoader(),
				new Class[] { proxyInterface }, new InvocationHandler() {
					
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						for(T plugin:plugins) {
							try {
								method.invoke(plugin, args);
							} catch (IllegalArgumentException e) {
								// Ignore
							} catch (Throwable e) {
								if (e instanceof InvocationTargetException && ((InvocationTargetException) e).getTargetException() instanceof PluginCriticalException) {
									throw ((InvocationTargetException) e).getTargetException();
								} else {
									logger.error("Error invoking method #" + method.getName() + " of plugin '" + plugin.getClass().getName() + "'" + "(" + e.toString() + ")", e);
								}
							} 
						}
						return null;
					}
				});
		return proxy;
	}
	
	public List<T> getPlugins() {
		return plugins;
	}

	public static <T> Builder<T> builder(Class<T> pluginClass) {
		return new Builder<T>(pluginClass);
	}
	
	public static class Builder<T> {
		
		private final Class<T> pluginClass;
		protected List<T> plugins = new ArrayList<>();
		private Predicate<T> pluginsFilter = null;
		
		public Builder(Class<T> pluginClass) {
			super();
			this.pluginClass = pluginClass;
		}
		
		public Builder<T> withPluginFilter(Predicate<T> pluginsFilter) {
			this.pluginsFilter = pluginsFilter;
			return this;
		}

		public Builder<T> withPlugin(T plugin) {
			plugins.add(plugin);
			return this;
		}
		
		public Builder<T> withPlugins(List<T> plugins_) {
			plugins.addAll(plugins_);
			return this;
		}
		
		public Builder<T> withPluginsFromClasspath() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
			return withPluginsFromClasspath(null);
		}
		
		public Builder<T> withPluginsFromClasspath(String packagePrefix) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
			List<T> pluginsFromClassLoader = getPluginsFromClassLoader(packagePrefix);
			plugins.addAll(pluginsFromClassLoader);
			return this;
		}
		
		private List<T> getPluginsFromClassLoader(String packagePrefix) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Set<Class<?>> classesWithAnnotation = CachedAnnotationScanner.getClassesWithAnnotation(packagePrefix, Plugin.class, cl);
			
			List<String> pluginClasses = new ArrayList<>();
			List<T> plugins = new ArrayList<>();
			for (Class<?> clazz : classesWithAnnotation) {
				if(pluginClass.isAssignableFrom(clazz)) {
					String className = clazz.getName();
					if(clazz.getAnnotation(IgnoreDuringAutoDiscovery.class) == null) {
						pluginClasses.add(className);
						@SuppressWarnings("unchecked")
						T plugin = newPluginInstance((Class<T>) clazz);
						plugins.add(plugin);
					} else {
						logger.debug("Ignoring plugin "+className+" annotated by "+IgnoreDuringAutoDiscovery.class.getName());
					}
				}
			}
			
			return plugins;
		}
		
		private T newPluginInstance(Class<T> _class) throws InstantiationException, IllegalAccessException  {
			T plugin = _class.newInstance();
			return (T) plugin;
		}
		
		/**
		 * Sort the plugins according to their mutual dependencies.
		 * The plugin with the highest dependency to other plugins will be located at the end of the list.
		 * 
		 * @param plugins the unsorted list of plugins
		 * @return the sorted list of plugins
		 * @throws CircularDependencyException if a circular dependency is detected
		 */
		private List<T> sortPluginsByDependencies(List<T> plugins) throws CircularDependencyException {
			// Create a list of additional dependencies based on the attribute "runsBefore"
			// The attribute "runsBefore" specifies a list of plugins before which a specific plugin should be executed.
			// Specifying that "A has to be run before B" has the same meaning as "B is depending on A"
			Map<Class<?>, List<Class<?>>> additionalDependencies = new HashMap<>();
			for (T plugin : plugins) {
				Class<?> pluginClass = plugin.getClass();
				Plugin annotation = pluginClass.getAnnotation(Plugin.class);
				if(annotation != null) {
					Class<?>[] runsBeforeList = annotation.runsBefore();
					for (Class<?> runsBefore : runsBeforeList) {
						// 
						additionalDependencies.computeIfAbsent(runsBefore, c->new ArrayList<>()).add(pluginClass);
					}
				}
			}
			
			List<T> result = new ArrayList<>(plugins);
			
			int iterationCount = 0;
			
			boolean hasModification = true;
			// loop as long as modifications to the ordering of the list are performed
			while(hasModification) {
				if(iterationCount>1000) {
					throw new CircularDependencyException("Circular dependency in the plugin dependencies");
				}
				
				hasModification = false;
				List<T> clone = new ArrayList<>(result);
				for (T plugin : result) {
					Class<?> pluginClass = plugin.getClass();
					Plugin annotation = pluginClass.getAnnotation(Plugin.class);
					
					final List<Class<?>> allDependencies = new ArrayList<>();

					if(annotation != null) {
						Class<?>[] dependencies = annotation.dependencies();
						allDependencies.addAll(Arrays.asList(dependencies));
					}
					
					if(additionalDependencies.containsKey(pluginClass)) {
						allDependencies.addAll(additionalDependencies.get(pluginClass));
					}
					
					int initialPosition = clone.indexOf(plugin);
					int newPosition = -1;
					if(allDependencies.size()>0) {
						for (Class<?> dependency : allDependencies) {
							int positionOfDependencyInClone = IntStream.range(0, clone.size()).filter(i -> dependency.equals(clone.get(i).getClass())).findFirst().orElse(-1);
							// if the dependency is located after the current plugin  
							if(positionOfDependencyInClone>initialPosition) {
								// if this is the highest position of all dependencies of this plugin
								if(positionOfDependencyInClone>newPosition) {
									newPosition = positionOfDependencyInClone;
								}
							}
						}
					}
					if(newPosition>=0) {
						// move the plugin after the dependency with the highest position
						clone.add(newPosition+1, plugin);
						clone.remove(initialPosition);
						hasModification = true;
					}
				}
				
				result = clone;
				iterationCount++;
			}
			
			return result;
		}
		
		@SuppressWarnings("serial")
		public static class CircularDependencyException extends Exception {

			public CircularDependencyException(String message) {
				super(message);
			}
			
		}
		
		public PluginManager<T> build() throws CircularDependencyException {
			List<T> validPlugins = plugins.stream()
									.filter(p->!(p instanceof OptionalPlugin) || ((OptionalPlugin)p).validate())
									.filter(p->pluginsFilter == null || pluginsFilter.test(p))
									.collect(Collectors.toList());		
			List<T> sortedPluginsByDependencies = sortPluginsByDependencies(validPlugins);
			return new PluginManager<T>(pluginClass, sortedPluginsByDependencies);
		}
	}
}
