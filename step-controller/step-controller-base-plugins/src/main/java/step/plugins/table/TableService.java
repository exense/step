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
package step.plugins.table;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.controller.services.async.AsyncTaskManager;
import step.controller.services.async.AsyncTaskStatus;
import step.core.GlobalContext;
import step.core.deployment.ApplicationServices;
import step.core.objectenricher.ObjectHookRegistry;
import step.framework.server.access.AccessManager;
import step.framework.server.security.Secured;
import step.framework.server.tables.TableRegistry;
import step.framework.server.tables.service.TableRequest;
import step.framework.server.tables.service.TableResponse;
import step.framework.server.tables.service.TableServiceException;
import step.resources.Resource;
import step.resources.ResourceManager;

@Singleton
@Path("table")
@Tag(name = "Tables")
public class TableService extends ApplicationServices {

    private static final Logger logger = LoggerFactory.getLogger(TableService.class);

    private step.framework.server.tables.service.TableService tableService;
    private AsyncTaskManager asyncTaskManager;
    private ResourceManager resourceManager;

    private int maxTime;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        TableRegistry tableRegistry = context.require(TableRegistry.class);
        ObjectHookRegistry objectHookRegistry = context.require(ObjectHookRegistry.class);
        AccessManager accessManager = context.require(AccessManager.class);
        maxTime = context.getConfiguration().getPropertyAsInteger("db.query.maxTime", 30);
        tableService = new step.framework.server.tables.service.TableService(tableRegistry, objectHookRegistry, accessManager);
        asyncTaskManager = getContext().require(AsyncTaskManager.class);
        resourceManager = context.getResourceManager();
    }

    @POST
    @Path("/{tableName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured
    public TableResponse<?> request(@PathParam("tableName") String tableName, TableRequest request) throws TableServiceException {
        return tableService.request(tableName, request, getSession());
    }

    @POST
    @Path("/{tableName}/export")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured
    public AsyncTaskStatus<Resource> createExport(@PathParam("tableName") String tableName, TableExportRequest exportRequest) throws Exception {
        return asyncTaskManager.scheduleAsyncTask(new TableExportTask(tableService, resourceManager, tableName, exportRequest, getSession()));
    }
}
