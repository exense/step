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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.automation.packages.accessor.AutomationPackageAccessorImpl;
import step.automation.packages.execution.AutomationPackageExecutor;
import step.automation.packages.execution.IsolatedAutomationPackageRepository;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.execution.model.ExecutionAccessor;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.scheduler.SchedulerPlugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.functions.accessor.FunctionAccessor;
import step.functions.manager.FunctionManager;
import step.functions.plugin.FunctionControllerPlugin;
import step.functions.type.FunctionTypeRegistry;
import step.resources.ResourceManagerControllerPlugin;

@Plugin(dependencies = {ObjectHookControllerPlugin.class, ResourceManagerControllerPlugin.class, FunctionControllerPlugin.class, SchedulerPlugin.class})
public class AutomationPackagePlugin extends AbstractControllerPlugin {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackagePlugin.class);

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);

        AutomationPackageAccessor packageAccessor = new AutomationPackageAccessorImpl(
                context.getCollectionFactory().getCollection("automationPackages", AutomationPackage.class)
        );
        context.put(AutomationPackageAccessor.class, packageAccessor);
        context.getEntityManager().register(new AutomationPackageEntity(packageAccessor));

        Collection<AutomationPackage> automationPackageCollection = context.getCollectionFactory().getCollection("automationPackages", AutomationPackage.class);

        Table<AutomationPackage> collection = new Table<>(automationPackageCollection, "automation-package-read", true);
        context.get(TableRegistry.class).register("automationPackages", collection);

        context.getServiceRegistrationCallback().registerService(AutomationPackageServices.class);

        // EE implementation of AbstractAutomationPackageReader can be used
        if (context.get(AbstractAutomationPackageReader.class) == null) {
            log.info("Using the OS implementation of automation package accessor");
            context.put(AbstractAutomationPackageReader.class, new AutomationPackageReaderOS());
        }
    }

    @Override
    public void afterInitializeData(GlobalContext context) throws Exception {
        super.afterInitializeData(context);

        if (context.get(AutomationPackageManager.class) == null) {
            log.info("Using the OS implementation of automation package manager");

            // moved to 'afterInitializeData' to have the schedule accessor in context
            AutomationPackageManager packageManager = new AutomationPackageManagerOS(
                    context.require(AutomationPackageAccessor.class),
                    context.require(FunctionManager.class),
                    context.require(FunctionAccessor.class),
                    context.getPlanAccessor(),
                    context.getResourceManager(),
                    context.getScheduleAccessor(),
                    context.getScheduler(),
                    context.require(AbstractAutomationPackageReader.class)
            );
            context.put(AutomationPackageManager.class, packageManager);

            AutomationPackageExecutor packageExecutor = new AutomationPackageExecutor(
                    context.getScheduler(),
                    context.require(ExecutionAccessor.class),
                    context.require(FunctionTypeRegistry.class),
                    context.require(FunctionAccessor.class),
                    context.require(IsolatedAutomationPackageRepository.class),
                    packageManager
            );
            context.put(AutomationPackageExecutor.class, packageExecutor);
        }
    }

    @Override
    public void serverStop(GlobalContext context) {
        super.serverStop(context);
        try {
            AutomationPackageManager automationPackageManager = context.get(AutomationPackageManager.class);
            if (automationPackageManager != null) {
                automationPackageManager.cleanup();
            }
        } catch (Exception e) {
            log.warn("Unable to finalize automaton package manager");
        }

        try {
            AutomationPackageExecutor executor = context.get(AutomationPackageExecutor.class);
            if (executor != null) {
                executor.shutdown();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted", e);
        }
    }
}
