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

import step.automation.packages.AutomationPackageHookRegistry;
import step.automation.packages.AutomationPackagePlugin;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.parameter.ParameterManager;
import step.parameter.automation.AutomationPackageParametersRegistration;
import step.plugins.parametermanager.ParameterManagerControllerPlugin;

@Plugin(dependencies= {AutomationPackagePlugin.class, ParameterManagerControllerPlugin.class})
public class AutomationPackageParameterPlugin extends AbstractControllerPlugin {

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        AutomationPackageParametersRegistration.registerParametersHooks(
                context.require(AutomationPackageHookRegistry.class),
                context.require(AutomationPackageSerializationRegistry.class),
                context.require(ParameterManager.class)
        );
    }

}
