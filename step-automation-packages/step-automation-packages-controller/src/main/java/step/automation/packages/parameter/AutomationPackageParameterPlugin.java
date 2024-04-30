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
package step.automation.packages.parameter;

import step.automation.packages.AutomationPackagePlugin;
import step.core.automation.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.hooks.AutomationPackageHookRegistry;
import step.automation.packages.hooks.AutomationPackageParameterHook;
import step.core.GlobalContext;
import step.core.accessors.Accessor;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.parameter.Parameter;
import step.parameter.automation.AutomationPackageParameterJsonSchema;
import step.parameter.automation.AutomationPackageParametersRegistration;
import step.plugins.parametermanager.ParameterManagerControllerPlugin;

@Plugin(dependencies= {AutomationPackagePlugin.class, ParameterManagerControllerPlugin.class})
public class AutomationPackageParameterPlugin extends AbstractControllerPlugin {

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        registerParametersHooks(context.require(AutomationPackageHookRegistry.class), context.require(AutomationPackageSerializationRegistry.class), (Accessor<Parameter>) context.require("ParameterAccessor"));
    }

    public static void registerParametersHooks(AutomationPackageHookRegistry hookRegistry, AutomationPackageSerializationRegistry serRegistry, Accessor<Parameter> parameterAccessor) {
        hookRegistry.register(
                AutomationPackageParameterJsonSchema.FIELD_NAME_IN_AP,
                new AutomationPackageParameterHook(parameterAccessor)
        );
        AutomationPackageParametersRegistration.registerSerialization(serRegistry);
    }
}
