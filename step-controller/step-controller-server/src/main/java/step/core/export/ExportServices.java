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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.deployment.Session;
import step.core.export.ExportTaskManager.ExportRunnable;
import step.core.export.ExportTaskManager.ExportStatus;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.objectenricher.ObjectPredicateFactory;
import step.core.plans.PlanAccessor;
import ch.exense.commons.core.model.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionContainer;

@Singleton
@Path("export")
public class ExportServices extends AbstractServices {
	
	ExportManager exportManager;
	
	ExportTaskManager exportTaskManager;
	
	ObjectPredicateFactory objectPredicateFactory;
	
	PlanAccessor accessor;
	
	ObjectHookRegistry objectHookRegistry;
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		accessor = getContext().getPlanAccessor();
		exportTaskManager = getContext().get(ExportTaskManager.class);
		objectPredicateFactory = getContext().get(ObjectPredicateFactory.class);
		exportManager = new ExportManager(getContext());
		objectHookRegistry = getContext().get(ObjectHookRegistry.class);
	}

	@GET
	@Path("/{entity}/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public ExportStatus exportEntityById(@PathParam("entity") String entity, @PathParam("id") String id, @QueryParam("recursively") boolean recursively, @QueryParam("filename") String filename,
										 @QueryParam("additionalEntities") List<String> additionalEntities) {
		Session session = getSession();
		Map<String,String> metadata = getMetadata();
		return exportTaskManager.createExportTask(new ExportRunnable() {
			@Override
			public Resource runExport() throws IOException {
				ResourceRevisionContainer resourceContainer = getResourceManager().createResourceContainer(ResourceManager.RESOURCE_TYPE_TEMP, filename);
				ExportConfiguration exportConfig = new ExportConfiguration(resourceContainer.getOutputStream(), metadata,objectPredicateFactory.getObjectPredicate(session),
						entity, recursively, additionalEntities);
				exportManager.exportById(exportConfig, id, objectPredicateFactory.getObjectPredicate(session));
				status.setWarnings(exportConfig.getMessages());
				resourceContainer.save(null);
				return resourceContainer.getResource();
			}
		});
	}
	
	private Map<String,String> getMetadata() {
		Map<String,String> metadata = new HashMap<String,String>();
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
		return exportTaskManager.createExportTask(new ExportRunnable() {
			@Override
			public Resource runExport() throws FileNotFoundException, IOException {
				ResourceRevisionContainer resourceContainer = getResourceManager().createResourceContainer(ResourceManager.RESOURCE_TYPE_TEMP, filename);
				ExportConfiguration exportConfig = new ExportConfiguration(resourceContainer.getOutputStream(),
						metadata, objectPredicateFactory.getObjectPredicate(session), entity, recursively, additionalEntities);
				exportManager.exportAll(exportConfig);
				status.setWarnings(exportConfig.getMessages());
				resourceContainer.save(null);
				return resourceContainer.getResource();
			}
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
