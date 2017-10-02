package step.core.export;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.AttachmentManager;
import step.core.artefacts.ArtefactAccessor;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;

@Singleton
@Path("import")
public class ImportServices extends AbstractServices {
	
	private static final Logger logger = LoggerFactory.getLogger(ImportServices.class);
		
	AttachmentManager attachmentManager;
	
	ImportManager importManager;
		
	@PostConstruct
	public void init() {
		ArtefactAccessor accessor = getContext().getArtefactAccessor();
		importManager = new ImportManager(accessor);
		attachmentManager = getContext().getAttachmentManager();
	}

	@POST
	@Path("/container/{id}/artefact")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public void importArtefact(@PathParam("id") String id) throws IOException {
		File file = attachmentManager.getFileById(id);
		importManager.importArtefacts(file);
	}
}
