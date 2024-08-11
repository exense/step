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
package step.automation.packages.execution;

import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackagePlugin;
import step.core.GlobalContext;
import step.core.execution.model.ExecutionAccessor;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;

@Plugin(dependencies = {AutomationPackagePlugin.class})
public class IsolatedAutomationPackageRepositoryPlugin extends AbstractControllerPlugin {

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);
    }

    @Override
    public void afterInitializeData(GlobalContext context) throws Exception {
        super.afterInitializeData(context);

        IsolatedAutomationPackageRepository repository = new IsolatedAutomationPackageRepository(
                context.require(AutomationPackageManager.class),
                context.require(FunctionTypeRegistry.class),
                context.require(FunctionAccessor.class)
        );

        context.getRepositoryObjectManager().registerRepository(AutomationPackageExecutor.ISOLATED_AUTOMATION_PACKAGE, repository);
        context.put(IsolatedAutomationPackageRepository.class, repository);

        AutomationPackageExecutor packageExecutor = new AutomationPackageExecutor(
                context.getScheduler(),
                context.require(ExecutionAccessor.class),
                repository
        );
        context.put(AutomationPackageExecutor.class, packageExecutor);
    }
}
