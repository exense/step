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

import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.hooks.AutomationPackageHookRegistry;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.deserialization.AutomationPackageSchedulesRegistration;
import step.automation.packages.hooks.ExecutionTaskParameterWithoutSchedulerHook;
import step.automation.packages.model.AutomationPackageSchedule;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.controller.ControllerSettingAccessor;
import step.core.controller.ControllerSettingPlugin;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.entities.EntityManager;
import step.core.objectenricher.ObjectEnricher;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.plugins.screentemplating.*;

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

		registerSchedulerHooks(context.require(AutomationPackageHookRegistry.class), context.require(AutomationPackageSerializationRegistry.class), scheduler);
	}

	public static void registerSchedulerHooks(AutomationPackageHookRegistry apRegistry, AutomationPackageSerializationRegistry serRegistry, ExecutionScheduler scheduler) {
		apRegistry.register(AutomationPackageSchedule.FIELD_NAME_IN_AP, new AutomationPackageSchedulerHook(scheduler));
		AutomationPackageSchedulesRegistration.registerSerialization(serRegistry);
	}

	public static class AutomationPackageSchedulerHook extends ExecutionTaskParameterWithoutSchedulerHook {

		private final ExecutionScheduler scheduler;

		public AutomationPackageSchedulerHook(ExecutionScheduler scheduler) {
			this.scheduler = scheduler;
		}

		@Override
		public void onCreate(List<? extends ExecutiontTaskParameters> entities, ObjectEnricher enricher, AutomationPackageManager manager) {
			for (ExecutiontTaskParameters entity : entities) {
				//make sure the execution parameter of the schedule are enriched too (required to execute in same project
				// as the schedule and populate event bindings
				enricher.accept(entity.getExecutionsParameters());
				scheduler.addExecutionTask(entity, false);
			}
		}

		@Override
		public void onDelete(AutomationPackage automationPackage, AutomationPackageManager manager) {
			List<ExecutiontTaskParameters> entities = manager.getPackageSchedules(automationPackage.getId());
			for (ExecutiontTaskParameters entity : entities) {
				scheduler.removeExecutionTask(entity.getId().toString());
			}
		}
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
