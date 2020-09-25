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
