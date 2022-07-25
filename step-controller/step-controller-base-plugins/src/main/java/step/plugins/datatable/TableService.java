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
package step.plugins.datatable;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.deployment.ApplicationServices;
import step.core.export.ExportTaskManager;
import step.core.export.ExportTaskManager.ExportStatus;
import step.core.objectenricher.ObjectHookRegistry;
import step.framework.server.security.Secured;
import step.framework.server.tables.TableRegistry;
import step.framework.server.tables.service.TableRequest;
import step.framework.server.tables.service.TableResponse;
import step.framework.server.tables.service.TableServiceException;

import java.util.List;

@Singleton
@Path("table")
@Tag(name = "Tables")
public class TableService extends ApplicationServices {
	
	private static final Logger logger = LoggerFactory.getLogger(TableService.class);

	private step.framework.server.tables.service.TableService tableService;
	protected int maxTime;

	protected ExportTaskManager exportTaskManager;

	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		TableRegistry tableRegistry = context.require(TableRegistry.class);
		ObjectHookRegistry objectHookRegistry = context.require(ObjectHookRegistry.class);
		maxTime = context.getConfiguration().getPropertyAsInteger("db.query.maxTime",30);
		tableService = new step.framework.server.tables.service.TableService(tableRegistry, objectHookRegistry);
		exportTaskManager = new ExportTaskManager(context.getResourceManager());
	}
	
	@PreDestroy
	public void destroy() {
	}
	
	@POST
	@Path("/{tableName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public TableResponse<?> request(@PathParam("tableName") String tableName, TableRequest request) throws TableServiceException {
		return tableService.request(tableName, request, getSession());
	}

	@GET
	@Path("/{id}/column/{column}/distinct")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public List<String> getTableColumnDistinct(@PathParam("id") String tableID, @PathParam("column") String column, @Context UriInfo uriInfo) throws Exception {
		// TODO reimplement
		return null;
	}

	@POST
	@Path("/{id}/searchIdsBy/{column}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured
	public List<String> searchIdsBy(@PathParam("id") String tableID, @PathParam("column") String columnName, String searchValue) throws Exception {
		// TODO reimplement
		return null;
	}

	@GET
	@Path("/{id}/export")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public String createExport(@PathParam("id") String tableID, @Context UriInfo uriInfo) throws Exception {
		// TODO reimplement
		return null;
	}
	
	@GET
	@Path("/exports/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public ExportStatus getExport(@PathParam("id") String reportID) throws Exception {
		return exportTaskManager.getExportStatus(reportID);
	}
}
