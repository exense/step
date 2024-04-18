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
package step.plugins.autoscaling;

import step.artefacts.handlers.functions.TokenAutoscalingExecutionPlugin;
import step.artefacts.handlers.functions.autoscaler.TokenAutoscalingDriver;
import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.AbstractWebPlugin;
import step.core.plugins.Ng2WebPlugin;
import step.core.plugins.Plugin;
import step.framework.server.ServiceRegistrationCallback;

@Plugin
public class TokenAutoscalingControllerPlugin extends AbstractControllerPlugin {

    private TokenAutoscalingDriver autoscalingDriver;

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        ServiceRegistrationCallback serviceRegistrationCallback = context.getServiceRegistrationCallback();
        serviceRegistrationCallback.registerService(TokenAutoscalingServices.class);
        serviceRegistrationCallback.registerService(TokenAutoscalingPlanServices.class);

        autoscalingDriver = TokenAutoscalingExecutionPlugin.createAutoscalingDriver(context.getConfiguration());
        context.put(TokenAutoscalingDriver.class, autoscalingDriver);
    }

    @Override
    public AbstractWebPlugin getWebPlugin() {
        // Enabling the web plugin only if a driver is configured
        if (autoscalingDriver != null) {
            Ng2WebPlugin webPlugin = new Ng2WebPlugin();
            webPlugin.setName("autoscaler");
            return webPlugin;
        } else {
            return null;
        }
    }
}
