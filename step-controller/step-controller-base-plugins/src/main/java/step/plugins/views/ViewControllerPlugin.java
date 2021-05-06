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
		ViewModelAccessor accessor = new ViewModelAccessorImpl(
				context.getCollectionFactory().getCollection("views", ViewModel.class));
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
