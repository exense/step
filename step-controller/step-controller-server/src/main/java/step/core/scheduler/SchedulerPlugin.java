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

import step.automation.packages.hooks.AutomationPackageHookRegistry;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.controller.ControllerSettingAccessor;
import step.core.controller.ControllerSettingPlugin;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.entities.EntityManager;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.plugins.screentemplating.*;
import step.automation.packages.hooks.AutomationPackageHook;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Plugin(dependencies= {ScreenTemplatePlugin.class, ControllerSettingPlugin.class, ObjectHookControllerPlugin.class})
public class SchedulerPlugin extends AbstractControllerPlugin {

	private ControllerSettingAccessor controllerSettingAccessor;

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		controllerSettingAccessor = context.require(ControllerSettingAccessor.class);
		Collection<ExecutiontTaskParameters> collectionDriver = context.getCollectionFactory().getCollection(EntityManager.tasks,
				ExecutiontTaskParameters.class);
		context.get(TableRegistry.class).register(EntityManager.tasks, new Table<>(collectionDriver, "task-read", true));

	}

	@Override
	public void migrateData(GlobalContext context) throws Exception {

	}

	@Override
	public void initializeData(GlobalContext context) throws Exception {
		createScreenInputDefinitionsIfNecessary(context);
		createSchedulerSettingsIfNecessary(context);
	}

	@Override
	public void afterInitializeData(GlobalContext context) throws Exception {
		ExecutionScheduler scheduler = new ExecutionScheduler(context.require(ControllerSettingAccessor.class), context.getScheduleAccessor(), new Executor(context));
		context.setScheduler(scheduler);

		AutomationPackageHookRegistry apRegistry = context.require(AutomationPackageHookRegistry.class);
		apRegistry.register(ExecutiontTaskParameters.class, new AutomationPackageHook<ExecutiontTaskParameters>() {

			@Override
			public void onCreate(ExecutiontTaskParameters entity) {
				scheduler.addExecutionTask(entity, false);
			}

			@Override
			public void onDelete(ExecutiontTaskParameters entity) {
				scheduler.removeExecutionTask(entity.getId().toString());
			}
		});
	}

	@Override
	public void serverStop(GlobalContext context) {

	}

	protected void createSchedulerSettingsIfNecessary(GlobalContext context) {
		controllerSettingAccessor.createSettingIfNotExisting(ExecutionScheduler.SETTING_SCHEDULER_ENABLED, "true");
	}

	protected void createScreenInputDefinitionsIfNecessary(GlobalContext context) {
		// Plan table
		ScreenInputAccessor screenInputAccessor = context.get(ScreenInputAccessor.class);
		List<ScreenInput> screenInputsByScreenId = screenInputAccessor.getScreenInputsByScreenId(ScreenTemplatePlugin.SCHEDULER_TABLE);
		Input nameInput = new Input(InputType.TEXT, "attributes.name", "Name", null, null);
		nameInput.setCustomUIComponents(List.of("schedulerTaskLink"));
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
			screenInputAccessor.save(new ScreenInput(0, ScreenTemplatePlugin.SCHEDULER_TABLE, nameInput));
		}
		
		if(screenInputsByScreenId.isEmpty()) {
			screenInputAccessor.save(new ScreenInput(1, ScreenTemplatePlugin.SCHEDULER_TABLE,
					new Input(InputType.TEXT, "executionsParameters.customParameters.env", "Environment", null, null)));
		}
	}
}
