package step.functions.packages;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.objectenricher.ObjectHookRegistry;
import step.functions.Function;

@Path("/functionpackages")
public class FunctionPackageServices extends AbstractServices {

	private static final Logger logger = LoggerFactory.getLogger(FunctionPackageServices.class);
	
	protected FunctionPackageManager functionPackageManager;
	private ObjectHookRegistry objectHookRegistry;
	
	@PostConstruct
	public void init() {
		functionPackageManager = getContext().get(FunctionPackageManager.class);
		objectHookRegistry = getContext().get(ObjectHookRegistry.class);
	}
	
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="kw-read")
	public FunctionPackage get(@PathParam("id") String functionPackageId) {
		return functionPackageManager.getFunctionPackage(functionPackageId);
	}
	
	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="kw-write")
	public void delete(@PathParam("id") String functionPackageId) {
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
	public FunctionPackage save(FunctionPackage functionPackage, @Context UriInfo uriInfo) throws Exception {
		return functionPackageManager.addOrUpdateFunctionPackage(functionPackage, objectHookRegistry.getObjectEnricher(getSession()));
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/resourcebased")
	@Secured(right="kw-write")
	public FunctionPackage update(FunctionPackage functionPackage, @Context UriInfo uriInfo) throws Exception {
		return functionPackageManager.addOrUpdateResourceBasedFunctionPackage(functionPackage, objectHookRegistry.getObjectEnricher(getSession()));
	}
	
	@GET
	@Path("/resourcebased/lookup/{resourceName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="kw-read")
	public FunctionPackage lookupByResourceName(@PathParam("resourceName") String resourceName) throws Exception {
		return functionPackageManager.getPackageByResourceName(resourceName);
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
	public FunctionPackage reload(@PathParam("id") String functionPackageId, @Context UriInfo uriInfo) throws Exception {
		return functionPackageManager.reloadFunctionPackage(functionPackageId, objectHookRegistry.getObjectEnricher(getSession()));
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
