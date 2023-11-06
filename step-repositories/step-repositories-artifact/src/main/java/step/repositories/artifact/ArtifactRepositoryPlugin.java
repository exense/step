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
import step.core.GlobalContext;
import step.core.controller.ControllerSettingAccessor;
import step.core.controller.ControllerSettingPlugin;
import step.core.plans.PlanAccessor;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.repositories.ArtifactRepositoryConstants;

@Plugin(dependencies = {ControllerSettingPlugin.class})
public class ArtifactRepositoryPlugin extends AbstractControllerPlugin {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactRepositoryPlugin.class);

    public static final String MAVEN_REPO_ID = ArtifactRepositoryConstants.MAVEN_REPO_ID;
    public static final String RESOURCE_REPO_ID = ArtifactRepositoryConstants.RESOURCE_REPO_ID;

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        PlanAccessor planAccessor = context.getPlanAccessor();
        ControllerSettingAccessor controllerSettingAccessor = context.require(ControllerSettingAccessor.class);
        Configuration configuration = context.getConfiguration();
        MavenArtifactRepository mavenRepository = new MavenArtifactRepository(planAccessor, context.getResourceManager(), controllerSettingAccessor, configuration);
        ResourceArtifactRepository resourceRepository = new ResourceArtifactRepository(planAccessor, context.getResourceManager());
        context.getRepositoryObjectManager().registerRepository(MAVEN_REPO_ID, mavenRepository);
        context.getRepositoryObjectManager().registerRepository(RESOURCE_REPO_ID, resourceRepository);
        super.serverStart(context);
    }
}
