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
package step.engine.plugins;

import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageReader;
import step.automation.packages.hooks.AutomationPackageHookRegistry;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.automation.deserialization.AutomationPackageSerializationRegistry;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.plugins.Plugin;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.resources.ResourceManager;

/**
 * Registers the automation package manager for local executions in execution engine context
 */
@Plugin(dependencies = {FunctionPlugin.class})
public class AutomationPackageOSPlugin extends AbstractExecutionEnginePlugin {

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
        if (context.getOperationMode() == OperationMode.LOCAL) {
            FunctionAccessor functionAccessor = context.require(FunctionAccessor.class);
            ResourceManager resourceManager = context.getResourceManager();

            AutomationPackageHookRegistry hookRegistry = context.computeIfAbsent(AutomationPackageHookRegistry.class, automationPackageHookRegistryClass -> new AutomationPackageHookRegistry());
            AutomationPackageSerializationRegistry serRegistry = context.computeIfAbsent(AutomationPackageSerializationRegistry.class, serRegistryClass -> new AutomationPackageSerializationRegistry());

            AutomationPackageReader reader = context.computeIfAbsent(AutomationPackageReader.class, automationPackageReaderClass -> new AutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, serRegistry));

            context.computeIfAbsent(
                   AutomationPackageManager.class,
                   automationPackageManagerClass -> AutomationPackageManager.createLocalAutomationPackageManager(
                           context.require(FunctionTypeRegistry.class),
                           functionAccessor,
                           resourceManager,
                           reader,
                           hookRegistry
                   )
           );
        }
    }

}
