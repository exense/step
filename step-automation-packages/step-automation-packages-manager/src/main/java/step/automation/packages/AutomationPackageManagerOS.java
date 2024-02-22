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

import org.bson.types.ObjectId;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.automation.packages.accessor.InMemoryAutomationPackageAccessorImpl;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.PlanAccessor;
import step.automation.packages.hooks.AutomationPackageHookRegistry;
import step.core.scheduler.ExecutionTaskAccessor;
import step.core.scheduler.InMemoryExecutionTaskAccessor;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.accessor.LayeredFunctionAccessor;
import step.functions.manager.FunctionManager;
import step.functions.manager.FunctionManagerImpl;
import step.functions.type.FunctionTypeRegistry;
import step.resources.LocalResourceManagerImpl;
import step.resources.ResourceManager;

import java.io.File;
import java.util.List;

public class AutomationPackageManagerOS extends AutomationPackageManager {
    public AutomationPackageManagerOS(AutomationPackageAccessor automationPackageAccessor, FunctionManager functionManager,
                                      FunctionAccessor functionAccessor, PlanAccessor planAccessor, ResourceManager resourceManager,
                                      ExecutionTaskAccessor executionTaskAccessor, AutomationPackageHookRegistry automationPackageHookRegistry,
                                      AbstractAutomationPackageReader<?> reader, AutomationPackageLocks automationPackageLocks) {
        super(automationPackageAccessor, functionManager,
                functionAccessor, planAccessor,
                resourceManager, executionTaskAccessor,
                automationPackageHookRegistry, reader, automationPackageLocks
        );
    }

    @Override
    public AutomationPackageManager createIsolated(ObjectId isolatedContextId, FunctionTypeRegistry functionTypeRegistry, FunctionAccessor mainFunctionAccessor) {
        return createIsolatedAutomationPackageManagerOS(isolatedContextId, functionTypeRegistry, mainFunctionAccessor, getPackageReader());
    }

    public static AutomationPackageManager createIsolatedAutomationPackageManagerOS(ObjectId isolatedContextId,
                                                                                    FunctionTypeRegistry functionTypeRegistry,
                                                                                    FunctionAccessor mainFunctionAccessor,
                                                                                    AbstractAutomationPackageReader<?> reader) {

        return AutomationPackageManagerOS.createIsolatedAutomationPackageManagerOS(isolatedContextId,
                functionTypeRegistry, mainFunctionAccessor,
                new LocalResourceManagerImpl(new File("resources_" + isolatedContextId.toString())),
                reader);
    }

    public static AutomationPackageManager createIsolatedAutomationPackageManagerOS(ObjectId isolatedContextId,
                                                                                    FunctionTypeRegistry functionTypeRegistry,
                                                                                    FunctionAccessor mainFunctionAccessor,
                                                                                    ResourceManager resourceManager,
                                                                                    AbstractAutomationPackageReader<?> reader) {
        InMemoryFunctionAccessorImpl inMemoryFunctionRepository = new InMemoryFunctionAccessorImpl();
        LayeredFunctionAccessor layeredFunctionAccessor = new LayeredFunctionAccessor(List.of(inMemoryFunctionRepository, mainFunctionAccessor));

        AutomationPackageManager automationPackageManager = new AutomationPackageManagerOS(
                new InMemoryAutomationPackageAccessorImpl(),
                new FunctionManagerImpl(layeredFunctionAccessor, functionTypeRegistry),
                layeredFunctionAccessor,
                new InMemoryPlanAccessor(),
                resourceManager,
                new InMemoryExecutionTaskAccessor(),
                new AutomationPackageHookRegistry(), reader,
                new AutomationPackageLocks(DEFAULT_READLOCK_TIMEOUT_SECONDS));
        automationPackageManager.isIsolated = true;
        return automationPackageManager;
    }

}
