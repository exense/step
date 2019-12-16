package step.core.export;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.FileResolver;
import step.attachments.FileResolver.FileHandle;
import step.core.artefacts.ArtefactAccessor;
import step.core.deployment.AbstractServices;
import step.core.deployment.ObjectEnrichers;
import step.core.deployment.Secured;

@Singleton
@Path("import")
public class ImportServices extends AbstractServices {
	
	private static final Logger logger = LoggerFactory.getLogger(ImportServices.class);
		
	FileResolver fileResolver;
	
	ImportManager importManager;
	ObjectEnrichers objectEnrichers;
		
	@PostConstruct
	public void init() throws Exception {
		super.init();
		ArtefactAccessor accessor = getContext().getArtefactAccessor();
		importManager = new ImportManager(accessor);
		fileResolver = getContext().get(FileResolver.class);
		objectEnrichers = getContext().get(ObjectEnrichers.class);
	}

	@POST
	@Path("/artefact")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public void importArtefact(@QueryParam("path") String path, @Context ContainerRequestContext crc) throws IOException {
		try (FileHandle file = fileResolver.resolveFileHandle(path)) {
			importManager.importArtefacts(file.getFile(), objectEnrichers.getObjectEnricher(getSession(crc)));
		}
	}
}
