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

import step.automation.packages.AutomationPackageHookRegistry;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageReader;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.plugins.Plugin;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.parameter.ParameterManager;
import step.parameter.automation.AutomationPackageParametersRegistration;
import step.resources.ResourceManager;

/**
 * Registers the automation package manager for local executions in execution engine context
 */
@Plugin(dependencies = {FunctionPlugin.class, ParameterManagerLocalPlugin.class})
public class AutomationPackageLocalOSPlugin extends AbstractExecutionEnginePlugin {

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
        if (context.getOperationMode() == OperationMode.LOCAL) {
            FunctionAccessor functionAccessor = context.require(FunctionAccessor.class);
            ResourceManager resourceManager = context.getResourceManager();

            AutomationPackageHookRegistry hookRegistry = context.computeIfAbsent(AutomationPackageHookRegistry.class, automationPackageHookRegistryClass -> new AutomationPackageHookRegistry());
            AutomationPackageSerializationRegistry serRegistry = context.computeIfAbsent(AutomationPackageSerializationRegistry.class, serRegistryClass -> new AutomationPackageSerializationRegistry());

            AutomationPackageParametersRegistration.registerParametersHooks(hookRegistry, serRegistry, context.require(ParameterManager.class));

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
