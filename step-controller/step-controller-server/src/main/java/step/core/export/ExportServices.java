package step.core.export;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import step.core.accessors.AbstractOrganizableObject;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.deployment.Session;
import step.core.export.ExportTaskManager.ExportRunnable;
import step.core.export.ExportTaskManager.ExportStatus;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plans.PlanAccessor;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionContainer;

@Singleton
@Path("export")
public class ExportServices extends AbstractServices {
	
	ExportManager exportManager;
	
	ExportTaskManager exportTaskManager;
	
	ObjectHookRegistry objectHookRegistry;
	
	PlanAccessor accessor;
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		accessor = getContext().getPlanAccessor();
		exportTaskManager = getContext().get(ExportTaskManager.class);
		objectHookRegistry = getContext().get(ObjectHookRegistry.class);
		exportManager = new ExportManager(accessor);
	}

	@GET
	@Path("/plan/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public ExportStatus exportArtefact(@PathParam("id") String id) {
		return exportTaskManager.createExportTask(new ExportRunnable() {
			@Override
			public Resource runExport() throws IOException {
				String planName = accessor.get(id).getAttributes().get(AbstractOrganizableObject.NAME);
				ResourceRevisionContainer resourceContainer = getResourceManager().createResourceContainer(ResourceManager.RESOURCE_TYPE_TEMP, planName + ".json");
				exportManager.exportPlan(id, resourceContainer.getOutputStream());
				resourceContainer.save(null);
				return resourceContainer.getResource();
			}
		});
	}
	
	@GET
	@Path("/plans")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public ExportStatus exportAllArtefacts() {
		Session session = getSession();
		return exportTaskManager.createExportTask(new ExportRunnable() {
			@Override
			public Resource runExport() throws FileNotFoundException, IOException {
				ResourceRevisionContainer resourceContainer = getResourceManager().createResourceContainer(ResourceManager.RESOURCE_TYPE_TEMP, "artefact_export.json");
				exportManager.exportAllPlans(resourceContainer.getOutputStream(), objectHookRegistry.getObjectFilter(session));
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
