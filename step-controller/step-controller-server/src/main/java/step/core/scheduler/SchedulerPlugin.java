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

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.controller.ControllerSettingAccessor;
import step.core.controller.ControllerSettingPlugin;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.entities.EntityConstants;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.scheduler.housekeeping.HousekeepingJobsManager;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.plugins.screentemplating.ScreenTemplatePlugin;

@Plugin(dependencies= {ScreenTemplatePlugin.class, ControllerSettingPlugin.class, ObjectHookControllerPlugin.class})
public class SchedulerPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(SchedulerPlugin.class);

	private ControllerSettingAccessor controllerSettingAccessor;
	private HousekeepingJobsManager housekeepingJobsManager;

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		controllerSettingAccessor = context.require(ControllerSettingAccessor.class);
		Collection<ExecutiontTaskParameters> collectionDriver = context.getCollectionFactory().getCollection(EntityConstants.tasks,
				ExecutiontTaskParameters.class);
		context.get(TableRegistry.class).register(EntityConstants.tasks, new Table<>(collectionDriver, "task-read", true));

		this.housekeepingJobsManager = new HousekeepingJobsManager(context.getConfiguration(), controllerSettingAccessor);
		context.put(HousekeepingJobsManager.class, housekeepingJobsManager);
		housekeepingJobsManager.start();
	}

	@Override
	public void migrateData(GlobalContext context) throws Exception {

	}

	@Override
	public void initializeData(GlobalContext context) throws Exception {
		createSchedulerSettingsIfNecessary(context);
	}

	@Override
	public void afterInitializeData(GlobalContext context) throws Exception {
		ExecutionScheduler scheduler = new ExecutionScheduler(context.require(ControllerSettingAccessor.class), context.getScheduleAccessor(), new Executor(context));
		context.setScheduler(scheduler);
	}

	@Override
	public void serverStop(GlobalContext context) {
		super.serverStop(context);
		try {
			if (housekeepingJobsManager != null) {
				housekeepingJobsManager.shutdown();
			}
		} catch (SchedulerException e) {
			logger.error("Unable to stop the housekeeping jobs scheduler",e);
		}
	}

	protected void createSchedulerSettingsIfNecessary(GlobalContext context) {
		controllerSettingAccessor.createSettingIfNotExisting(ExecutionScheduler.SETTING_SCHEDULER_ENABLED, "true");
	}

}
