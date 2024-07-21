package step.functions.packages;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.deployment.AbstractStepServices;
import step.framework.server.security.Secured;
import step.functions.Function;

import java.util.List;

@Path("/functionpackages")
@Tag(name = "Keyword packages")
public class FunctionPackageServices extends AbstractStepServices {

	private static final Logger logger = LoggerFactory.getLogger(FunctionPackageServices.class);
	
	protected FunctionPackageManager functionPackageManager;
	
	@PostConstruct
	public void init() {
		functionPackageManager = getContext().get(FunctionPackageManager.class);
	}
	
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="kw-read")
	public FunctionPackage getFunctionPackage(@PathParam("id") String functionPackageId) {
		return functionPackageManager.getFunctionPackage(functionPackageId);
	}
	
	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="kw-write")
	public void deleteFunctionPackage(@PathParam("id") String functionPackageId) {
		functionPackageManager.removeFunctionPackage(functionPackageId);
	}
	

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/preview")
	@Secured(right="kw-write")
	public PackagePreview packagePreview(FunctionPackage functionPackage) {
		try {
			List<Function> functions = functionPackageManager.getPackagePreview(functionPackage);
			return new PackagePreview(functions);
		} catch (Exception e) {
			logger.warn("Error while loading package preview for function package "+functionPackage, e);
			return new PackagePreview(e.getMessage());
		}
	}
	
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="kw-write")
	public FunctionPackage saveFunctionPackage(FunctionPackage functionPackage, @Context UriInfo uriInfo) throws Exception {
		return functionPackageManager.addOrUpdateFunctionPackage(functionPackage);
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/resourcebased")
	@Secured(right = "kw-write")
	public FunctionPackage updateFunctionPackage(FunctionPackage functionPackage, @Context UriInfo uriInfo) throws Exception {
		throw new Exception(
				"This service has been removed. Use POST /rest/functionpackages/ instead. Lookup by resourceName isn't supported anymore");
	}

	@GET
	@Path("/resourcebased/lookup/{resourceName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right = "kw-read")
	public FunctionPackage lookupByResourceName(@PathParam("resourceName") String resourceName) throws Exception {
		throw new Exception(
				"This service has been removed. Lookup by resourceName isn't supported anymore. Use GET /rest/functionpackages/{id} instead.");
	}
	
	@GET
	@Path("/{id}/functions")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="kw-write")
	public List<Function> getPackageFunctions(@PathParam("id") String functionPackageId) {
		return functionPackageManager.getPackageFunctions(functionPackageId);
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{id}/reload")
	@Secured(right="kw-write")
	public FunctionPackage reloadFunctionPackage(@PathParam("id") String functionPackageId, @Context UriInfo uriInfo) throws Exception {
		return functionPackageManager.reloadFunctionPackage(functionPackageId);
	}
	
	public static class PackagePreview {
		
		public PackagePreview(List<Function> functions) {
			super();
			this.functions = functions;
		}

		public PackagePreview(String loadingError) {
			super();
			this.loadingError = loadingError;
		}

		private String loadingError;
		private List<Function> functions;

		public String getLoadingError() {
			return loadingError;
		}

		public void setLoadingError(String loadingError) {
			this.loadingError = loadingError;
		}

		public List<Function> getFunctions() {
			return functions;
		}

		public void setFunctions(List<Function> functions) {
			this.functions = functions;
		}
	}
}
