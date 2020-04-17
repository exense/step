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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.attachments.FileResolver.FileHandle;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plans.PlanAccessor;

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
		PlanAccessor accessor = getContext().getPlanAccessor();
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
			importManager.importPlans(file.getFile(), objectHookRegistry.getObjectEnricher(getSession()));
		} catch (Exception e) {
			logger.error("Import failed",e);
			throw e;
		}
	}
}
