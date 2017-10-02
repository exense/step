package step.core.export;

import java.io.File;
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

@Singleton
@Path("export")
public class ExportServices extends AbstractServices {
	
	private static final Logger logger = LoggerFactory.getLogger(ExportServices.class);
		
	ExportManager exportManager;
	
	ExportTaskManager exportTaskManager;
	
	@PostConstruct
	public void init() {
		ArtefactAccessor accessor = getContext().getArtefactAccessor();
		exportTaskManager = new ExportTaskManager(getContext().getAttachmentManager());
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
			public void runExport() throws FileNotFoundException, IOException {
				File export = new File(getContainer().getAbsolutePath()+"/artefact_export.json");
				exportManager.exportArtefactWithChildren(id, export);
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
			public void runExport() throws FileNotFoundException, IOException {
				File export = new File(getContainer().getAbsolutePath()+"/artefacts_export.json");
				exportManager.exportAllArtefacts(export);
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
