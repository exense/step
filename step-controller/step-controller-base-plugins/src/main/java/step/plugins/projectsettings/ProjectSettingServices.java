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
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import step.controller.services.async.AsyncTaskStatus;
import step.controller.services.entities.AbstractEntityServices;
import step.core.deployment.ControllerServiceException;
import step.framework.server.security.Secured;
import step.framework.server.security.SecuredContext;
import step.framework.server.tables.service.bulk.TableBulkOperationReport;
import step.framework.server.tables.service.bulk.TableBulkOperationRequest;
import step.projectsettings.ProjectSetting;
import step.projectsettings.ProjectSettingManager;

import java.util.List;

@Path("/project-settings")
@Tag(name = "ProjectSettings")
@Tag(name = "Entity=ProjectSetting")
@SecuredContext(key = "entity", value = "project-setting")
public class ProjectSettingServices extends AbstractEntityServices<ProjectSetting> {

    private ProjectSettingManager manager;

    public ProjectSettingServices() {
        super(ProjectSetting.ENTITY_NAME);
    }

    @PostConstruct
    public void init() throws Exception {
        super.init();
        manager = getContext().require(ProjectSettingManager.class);
    }

    @Override
    protected ProjectSetting beforeSave(ProjectSetting entity) {
        getObjectOverlapper().onBeforeSave(entity);
        return super.beforeSave(entity);
    }

    @GET
    @Path("/unique/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public List<ProjectSetting> getUniqueSettings() {
        try {
            return manager.getAllSettingsWithUniqueKeys(getObjectOverlapper());
        } catch (Exception e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    @Override
    public AsyncTaskStatus<TableBulkOperationReport> cloneEntities(TableBulkOperationRequest request) {
        throw new UnsupportedOperationException("Clone is not supported for project settings");
    }

    @Override
    public ProjectSetting clone(String id) {
        throw new UnsupportedOperationException("Clone is not supported for project settings");
    }

    @Override
    protected ProjectSetting cloneEntity(ProjectSetting entity) {
        throw new UnsupportedOperationException("Clone is not supported for project settings");
    }
}
