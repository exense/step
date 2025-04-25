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
package step.plugins.projectsettings;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Path;
import step.controller.services.entities.AbstractEntityServices;
import step.framework.server.security.SecuredContext;
import step.projectsettings.ProjectSetting;

@Path("/project-settings")
@Tag(name = "ProjectSettings")
@Tag(name = "Entity=ProjectSetting")
@SecuredContext(key = "entity", value = "project-setting")
public class ProjectSettingServices extends AbstractEntityServices<ProjectSetting> {

    public ProjectSettingServices() {
        super(ProjectSetting.ENTITY_NAME);
    }

}
