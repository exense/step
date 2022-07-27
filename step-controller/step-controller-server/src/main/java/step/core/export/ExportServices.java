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
package step.core.export;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import step.core.GlobalContext;
import step.core.deployment.AbstractStepServices;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plans.PlanAccessor;
import step.framework.server.Session;
import step.framework.server.security.Secured;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@Path("export")
@Tag(name = "Exports")
public class ExportServices extends AbstractStepServices {
	
	private ExportManager exportManager;
	private ExportTaskManager exportTaskManager;
	private PlanAccessor accessor;
	private ObjectHookRegistry objectHookRegistry;
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		accessor = context.getPlanAccessor();
		exportTaskManager = context.get(ExportTaskManager.class);
		exportManager = new ExportManager(context.getEntityManager(), context.getResourceManager());
		objectHookRegistry = context.get(ObjectHookRegistry.class);
	}

	@GET
	@Path("/{entity}/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public ExportStatus exportEntityById(@PathParam("entity") String entity, @PathParam("id") String id, @QueryParam("recursively") boolean recursively, @QueryParam("filename") String filename,
										 @QueryParam("additionalEntities") List<String> additionalEntities) {
		Session session = getSession();
		Map<String, String> metadata = getMetadata();
		String finalFilename = removeUnsupportedChars(filename);
		return exportTaskManager.createExportTask(handle -> {
			ResourceRevisionContainer resourceContainer = handle.getResourceManager().createResourceContainer(ResourceManager.RESOURCE_TYPE_TEMP, finalFilename);
			ExportConfiguration exportConfig = new ExportConfiguration(resourceContainer.getOutputStream(), metadata, objectHookRegistry.getObjectPredicate(session),
					entity, recursively, additionalEntities);
			ExportResult exportResult = exportManager.exportById(exportConfig, id);
			handle.setWarnings(exportResult.getMessages());
			resourceContainer.save(null);
			return resourceContainer.getResource();
		});
	}

	private String removeUnsupportedChars(String filename) {
		return filename.replace("/","").replace("\\","");
	}

	private Map<String,String> getMetadata() {
		Map<String,String> metadata = new HashMap<>();
		metadata.put("user",getSession().getUser().getUsername());
		metadata.put("version",getContext().getCurrentVersion().toString());
		metadata.put("export-time",String.valueOf(System.currentTimeMillis()));
		return metadata;
	}
	
	@GET
	@Path("/{entity}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public ExportStatus exportEntities(@PathParam("entity") String entity, @QueryParam("recursively") boolean recursively, @QueryParam("filename") String filename,
			@QueryParam("additionalEntities") List<String> additionalEntities) {
		Session session = getSession();
		Map<String,String> metadata = getMetadata();
		String finalFilename = removeUnsupportedChars(filename);
		return exportTaskManager.createExportTask(handle -> {
			ResourceRevisionContainer resourceContainer = handle.getResourceManager().createResourceContainer(ResourceManager.RESOURCE_TYPE_TEMP, finalFilename);
			ExportConfiguration exportConfig = new ExportConfiguration(resourceContainer.getOutputStream(),
					metadata, objectHookRegistry.getObjectPredicate(session), entity, recursively, additionalEntities);
			ExportResult exportResult = exportManager.exportAll(exportConfig);
			handle.setWarnings(exportResult.getMessages());
			resourceContainer.save(null);
			return resourceContainer.getResource();
		});
	}

	@GET
	@Path("/{id}/status")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ExportStatus getExportStatus(@PathParam("id") String id) {
		return exportTaskManager.getExportStatus(id);
	}
}
