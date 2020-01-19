package step.core.export;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import step.attachments.FileResolver;
import step.attachments.FileResolver.FileHandle;
import step.core.artefacts.ArtefactAccessor;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.objectenricher.ObjectHookRegistry;

@Singleton
@Path("import")
public class ImportServices extends AbstractServices {
	
	FileResolver fileResolver;
	
	ImportManager importManager;
	ObjectHookRegistry objectHookRegistry;
		
	@PostConstruct
	public void init() throws Exception {
		super.init();
		ArtefactAccessor accessor = getContext().getArtefactAccessor();
		importManager = new ImportManager(accessor);
		fileResolver = getContext().get(FileResolver.class);
		objectHookRegistry = getContext().get(ObjectHookRegistry.class);
	}

	@POST
	@Path("/artefact")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public void importArtefact(@QueryParam("path") String path) throws IOException {
		try (FileHandle file = fileResolver.resolveFileHandle(path)) {
			importManager.importArtefacts(file.getFile(), objectHookRegistry.getObjectEnricher(getSession()));
		}
	}
}
