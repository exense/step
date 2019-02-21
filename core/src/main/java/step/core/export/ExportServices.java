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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.artefacts.ArtefactAccessor;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.export.ExportTaskManager.ExportRunnable;
import step.core.export.ExportTaskManager.ExportStatus;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionContainer;

@Singleton
@Path("export")
public class ExportServices extends AbstractServices {
	
	private static final Logger logger = LoggerFactory.getLogger(ExportServices.class);
		
	ExportManager exportManager;
	
	ExportTaskManager exportTaskManager;
	
	@PostConstruct
	public void init() {
		ArtefactAccessor accessor = getContext().getArtefactAccessor();
		exportTaskManager = new ExportTaskManager(getContext().get(ResourceManager.class));
		exportManager = new ExportManager(accessor);
	}

	@GET
	@Path("/artefact/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public ExportStatus exportArtefact(@PathParam("id") String id) {
		return exportTaskManager.createExportTask(new ExportRunnable() {
			@Override
			public Resource runExport() throws IOException {
				ResourceRevisionContainer resourceContainer = getResourceManager().createResourceContainer("temp", "artefact_export.json");
				exportManager.exportArtefactWithChildren(id, resourceContainer.getOutputStream());
				resourceContainer.save();
				return resourceContainer.getResource();
			}
		});
	}
	
	@GET
	@Path("/artefacts")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public ExportStatus exportAllArtefacts() {
		return exportTaskManager.createExportTask(new ExportRunnable() {
			@Override
			public Resource runExport() throws FileNotFoundException, IOException {
				ResourceRevisionContainer resourceContainer = getResourceManager().createResourceContainer("temp", "artefact_export.json");
				exportManager.exportAllArtefacts(resourceContainer.getOutputStream());
				resourceContainer.save();
				return resourceContainer.getResource();
			}
		});
	}
	
	
	
	@GET
	@Path("/{id}/status")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public ExportStatus getExportStatus(@PathParam("id") String id) {
		return exportTaskManager.getExportStatus(id);
	}
}
