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
package step.automation.packages;

import step.automation.packages.accessor.AutomationPackageAccessor;
import step.automation.packages.accessor.AutomationPackageAccessorImpl;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.scheduler.SchedulerPlugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.functions.accessor.FunctionAccessor;
import step.functions.manager.FunctionManager;
import step.functions.plugin.FunctionControllerPlugin;
import step.resources.ResourceManagerControllerPlugin;

@Plugin(dependencies= {ObjectHookControllerPlugin.class, ResourceManagerControllerPlugin.class, FunctionControllerPlugin.class, SchedulerPlugin.class})
public class AutomationPackagePlugin extends AbstractControllerPlugin {

    private AutomationPackageManager packageManager;
    private AutomationPackageAccessor packageAccessor;

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);
        packageAccessor = new AutomationPackageAccessorImpl(
                context.getCollectionFactory().getCollection("automationPackages", AutomationPackage.class)
        );

        Collection<AutomationPackage> automationPackageCollection = context.getCollectionFactory().getCollection("automationPackages", AutomationPackage.class);

        Table<AutomationPackage> collection = new Table<>(automationPackageCollection, "autopack-read", true);
        context.get(TableRegistry.class).register("automationPackages", collection);

        context.getServiceRegistrationCallback().registerService(AutomationPackageServices.class);

        context.getEntityManager().register(new AutomationPackageEntity(packageAccessor));
    }

    @Override
    public void afterInitializeData(GlobalContext context) throws Exception {
        super.afterInitializeData(context);

        // moved to 'afterInitializeData' to have the schedule accessor in context
        packageManager = new AutomationPackageManager(
                packageAccessor, context.get(FunctionManager.class), context.get(FunctionAccessor.class), context.getPlanAccessor(), context.getResourceManager(),
                context.getScheduleAccessor(), context.getScheduler()
        );
        context.put(AutomationPackageManager.class, packageManager);

    }
}
