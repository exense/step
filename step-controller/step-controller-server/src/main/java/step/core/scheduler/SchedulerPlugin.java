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
package step.core.scheduler;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.controller.ControllerSettingAccessor;
import step.core.controller.ControllerSettingPlugin;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.tables.AbstractTable;
import step.core.tables.TableRegistry;
import step.plugins.screentemplating.Input;
import step.plugins.screentemplating.InputType;
import step.plugins.screentemplating.ScreenInput;
import step.plugins.screentemplating.ScreenInputAccessor;
import step.plugins.screentemplating.ScreenTemplatePlugin;
import step.resources.Resource;

@Plugin(dependencies= {ScreenTemplatePlugin.class, ControllerSettingPlugin.class})
public class SchedulerPlugin extends AbstractControllerPlugin {

	private static final String SCHEDULER_TABLE = "schedulerTable";
	
	private ControllerSettingAccessor controllerSettingAccessor;


	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		controllerSettingAccessor = context.require(ControllerSettingAccessor.class);
		Collection<ExecutiontTaskParameters> collectionDriver = context.getCollectionFactory().getCollection("tasks",
				ExecutiontTaskParameters.class);
		context.get(TableRegistry.class).register("tasks", new AbstractTable<>(collectionDriver, true));

	}
	
	@Override
	public void initializeData(GlobalContext context) throws Exception {
		createScreenInputDefinitionsIfNecessary(context);
		createSchedulerSettingsIfNecessary(context);
	}
	
	protected void createSchedulerSettingsIfNecessary(GlobalContext context) {
		controllerSettingAccessor.createSettingIfNotExisting(ExecutionScheduler.SETTING_SCHEDULER_ENABLED, "true");
	}

	protected void createScreenInputDefinitionsIfNecessary(GlobalContext context) {
		// Plan table
		ScreenInputAccessor screenInputAccessor = context.get(ScreenInputAccessor.class);
		List<ScreenInput> screenInputsByScreenId = screenInputAccessor.getScreenInputsByScreenId(SCHEDULER_TABLE);
		Input nameInput = new Input(InputType.TEXT, "attributes.name", "Name", null, null);
		nameInput.setValueHtmlTemplate("<entity-icon entity=\"stBean\" entity-name=\"'tasks'\"/> <scheduler-task-link scheduler-task=\"stBean\" />");
		AtomicBoolean inputExists = new AtomicBoolean(false);
		// Force content of input 'attributes.name'
		screenInputsByScreenId.forEach(i->{
			Input input = i.getInput();
			if(input.getId().equals("attributes.name")) {
				i.setInput(nameInput);
				screenInputAccessor.save(i);
				inputExists.set(true);
			}
		});
		// Create it if not existing
		if(!inputExists.get()) {
			screenInputAccessor.save(new ScreenInput(0, SCHEDULER_TABLE, nameInput));
		}
		
		if(screenInputsByScreenId.isEmpty()) {
			screenInputAccessor.save(new ScreenInput(1, SCHEDULER_TABLE, new Input(InputType.TEXT, "executionsParameters.customParameters.env", "Environment", null, null)));
		}
	}
}
