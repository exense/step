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
package step.automation.packages.scheduler;

import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageContext;
import step.core.scheduler.automation.AutomationPackageScheduleRegistration;
import step.automation.packages.AutomationPackageHookRegistry;
import step.core.scheduler.automation.AutomationPackageSchedule;
import step.core.GlobalContext;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.scheduler.ExecutionScheduler;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.scheduler.SchedulerPlugin;

import java.util.List;

@Plugin(dependencies= {SchedulerPlugin.class})
public class AutomationPackageSchedulerPlugin extends AbstractControllerPlugin {

	@Override
	public void afterInitializeData(GlobalContext context) throws Exception {
		registerSchedulerHooks(context.require(AutomationPackageHookRegistry.class), context.require(AutomationPackageSerializationRegistry.class), context.getScheduler());
	}

	public static void registerSchedulerHooks(AutomationPackageHookRegistry apRegistry,
											  AutomationPackageSerializationRegistry serRegistry,
											  ExecutionScheduler scheduler) {
		apRegistry.register(AutomationPackageSchedule.FIELD_NAME_IN_AP, new AutomationPackageSchedulerHook(scheduler));
		AutomationPackageScheduleRegistration.registerSerialization(serRegistry);
	}
}
