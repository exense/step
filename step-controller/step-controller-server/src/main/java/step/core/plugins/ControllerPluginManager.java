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
import java.util.stream.Stream;

import step.engine.plugins.ExecutionEnginePlugin;
import step.framework.server.ServerPluginManager;

public class ControllerPluginManager  {

	List<ControllerPlugin> controllerPluginStream;

	public ControllerPluginManager(ServerPluginManager parentPluginManager) {
		Stream<ControllerPlugin> stream = parentPluginManager.getPluginManager().getPlugins().stream().filter(ControllerPlugin.class::isInstance).map(ControllerPlugin.class::cast);
		controllerPluginStream = stream.collect(Collectors.toList());
	}

	public List<ExecutionEnginePlugin> getExecutionEnginePlugins() {
		return controllerPluginStream.stream().map(ControllerPlugin::getExecutionEnginePlugin).filter(Objects::nonNull).collect(Collectors.toList());
	}
	
	public List<WebPlugin> getWebPlugins() {
		return controllerPluginStream.stream().map(ControllerPlugin::getWebPlugin).filter(Objects::nonNull).collect(Collectors.toList());
	}


}
