package step.core.imports;

import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.FileResolver;
import step.attachments.FileResolver.FileHandle;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.objectenricher.ObjectHookRegistry;

@Singleton
@Path("import")
public class ImportServices extends AbstractServices {
	
	private static final Logger logger = LoggerFactory.getLogger(ImportServices.class);
	
	FileResolver fileResolver;
	
	ImportManager importManager;
	ObjectHookRegistry objectHookRegistry;
		
	@PostConstruct
	public void init() throws Exception {
		super.init();
		importManager = new ImportManager(getContext());
		fileResolver = getContext().getFileResolver();
		objectHookRegistry = getContext().get(ObjectHookRegistry.class);
	}

	@POST
	@Path("/{entity}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public void importEntity(@PathParam("entity") String entity, @QueryParam("path") String path) throws Exception {
		try (FileHandle file = fileResolver.resolveFileHandle(path)) {
			importManager.importAll(file.getFile(), objectHookRegistry.getObjectEnricher(getSession()),Arrays.asList(entity));
		} catch (Exception e) {
			logger.error("Import failed",e);
			throw e;
		}
	}
}
