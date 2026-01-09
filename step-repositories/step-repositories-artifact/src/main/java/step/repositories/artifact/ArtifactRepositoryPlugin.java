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
package step.repositories.artifact;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackagePlugin;
import step.core.GlobalContext;
import step.core.controller.ControllerSettingAccessor;
import step.core.controller.ControllerSettingPlugin;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.parameter.ParameterManager;
import step.plugins.parametermanager.ParameterManagerPlugin;
import step.repositories.ArtifactRepositoryConstants;

@Plugin(dependencies = {ControllerSettingPlugin.class, AutomationPackagePlugin.class, ParameterManagerPlugin.class, ControllerSettingPlugin.class})
public class ArtifactRepositoryPlugin extends AbstractControllerPlugin {

    @Override
    public void afterInitializeData(GlobalContext context) throws Exception {
        super.afterInitializeData(context);

        ControllerSettingAccessor controllerSettingAccessor = context.require(ControllerSettingAccessor.class);
        Configuration configuration = context.getConfiguration();
        MavenArtifactRepository mavenRepository = new MavenArtifactRepository(
                context.require(AutomationPackageManager.class),
                context.require(FunctionTypeRegistry.class),
                context.require(FunctionAccessor.class),
                configuration, controllerSettingAccessor,
                context.getResourceManager());
        ResourceArtifactRepository resourceRepository = new ResourceArtifactRepository(
                context.getResourceManager(),
                context.require(AutomationPackageManager.class),
                context.require(FunctionTypeRegistry.class),
                context.require(FunctionAccessor.class));
        context.getRepositoryObjectManager().registerRepository(ArtifactRepositoryConstants.MAVEN_REPO_ID, mavenRepository);
        context.getRepositoryObjectManager().registerRepository(ArtifactRepositoryConstants.RESOURCE_REPO_ID, resourceRepository);
    }
}
