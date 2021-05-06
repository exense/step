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
package step.core.controller;

import step.core.GlobalContext;
import step.core.execution.ExecutionContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.variables.VariableType;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.ExecutionEnginePlugin;

@Plugin
public class ControllerSettingPlugin extends AbstractControllerPlugin {

	private ControllerSettingAccessor controllerSettingAccessor;

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		controllerSettingAccessor = new ControllerSettingAccessorImpl(
				context.getCollectionFactory().getCollection("settings", ControllerSetting.class));
		context.put(ControllerSettingAccessor.class, controllerSettingAccessor);
	}

	@Override
	public ExecutionEnginePlugin getExecutionEnginePlugin() {
		return new AbstractExecutionEnginePlugin() {			
			@Override
			public void executionStart(ExecutionContext context) {
				context.getVariablesManager().putVariable(context.getCurrentReportNode(), VariableType.IMMUTABLE, "controllerSettings", 
						new ControllerSettingsService(controllerSettingAccessor));
			}
		};
	}

	public static class ControllerSettingsService {
		
		private ControllerSettingAccessor controllerSettingAccessor;

		public ControllerSettingsService(ControllerSettingAccessor controllerSettingAccessor) {
			super();
			this.controllerSettingAccessor = controllerSettingAccessor;
		}

		public ControllerSetting getSettingByKey(String key) {
			return controllerSettingAccessor.getSettingByKey(key);
		}
	}
}
