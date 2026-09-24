package step.ide.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackageUpdateResult;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.ControllerServiceException;
import step.ide.LocalIDEModel;
import step.ide.exceptions.FileExistsException;
import step.ide.exceptions.InvalidRequestException;

import java.util.List;

@Path("/local/ide")
@Tag(name = "IDE")
public class LocalIDEServices extends AbstractStepServices {

    private static final Logger logger = LoggerFactory.getLogger(LocalIDEServices.class);

    @PostConstruct
    public void init() throws Exception {
        super.init();
    }

    private static ControllerServiceException error(String message, Response.Status status) {
        return new ControllerServiceException(status.getStatusCode(), message);
    }

    private static ControllerServiceException error(String message, Response.Status status, Throwable cause) {
        return new ControllerServiceException(status.getStatusCode(), message, cause);
    }

    @POST
    @Path("ap/use-existing")
    @Consumes(MediaType.APPLICATION_JSON)
    public void useExistingAP(@QueryParam("directory") String directory) {
        if (directory == null || directory.isBlank()) {
            throw error("directory must not be empty", Response.Status.BAD_REQUEST);
        }
        // Workaround for windows: Strip the leading slash if it looks like /C:
        if (directory.startsWith("/") && directory.length() > 2 && directory.charAt(2) == ':') {
            directory = directory.substring(1);
        }

        java.nio.file.Path apPath;
        try {
            apPath = java.nio.file.Path.of(directory);
        } catch (java.nio.file.InvalidPathException e) {
            throw error("Invalid directory path: " + e.getMessage(), Response.Status.BAD_REQUEST);
        }
        var ideState = model();

        try {
            ideState.validateExistingAutomationPackageDirectory(apPath);
        } catch (Exception e) {
            throw error(e.getMessage(), Response.Status.BAD_REQUEST);
        }
        try {
            ideState.useExistingAutomationPackageDirectory(apPath);
        } catch (Exception e) {
            // Catch anything else (e.g., actual IO read errors during setup) as 500 Internal Error
            logger.error("Unable to use existing AP directory: {}", directory, e);
            throw error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private static LocalIDEModel model() {
        return LocalIDEModel.get();
    }

    @POST
    @Path("ap/initialize-new")
    @Consumes(MediaType.APPLICATION_JSON)
    public void initializeNewAP(@QueryParam("existingEmptyDirectory") String existingEmptyDirectory, @QueryParam("apName") String apName) {
        if (existingEmptyDirectory == null || existingEmptyDirectory.isBlank()) {
            throw error("existingEmptyDirectory is required", Response.Status.BAD_REQUEST);
        }

        java.nio.file.Path path;
        try {
            path = java.nio.file.Path.of(existingEmptyDirectory);
        } catch (java.nio.file.InvalidPathException e) {
            throw error("Invalid directory path: " + e.getMessage(), Response.Status.BAD_REQUEST);
        }

        try {
            // more validation
            try {
                model().validateInitializableAutomationPackageDirectory(path, false);
            } catch (FileExistsException e) {
                throw error(
                    "Directory already contains an automation package descriptor, refusing to overwrite: " + e.existingPath.toAbsolutePath(),
                    Response.Status.BAD_REQUEST
                );
            }
        } catch (IllegalArgumentException e) {
            throw error(e.getMessage(), Response.Status.BAD_REQUEST);
        }

        try {
            model().useNewAutomationPackageDirectory(path, apName);
        } catch (Exception e) {
            logger.error("Unable to initialize new AP directory: {}", path.toAbsolutePath(), e);
            throw error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, e);
        }

    }

    public record AutomationPackageDescriptor(String directory, String name) {
    }

    @GET
    @Path("ap/current")
    @Produces(MediaType.APPLICATION_JSON)
    public AutomationPackageDescriptor getCurrentAP() {
        var dir = model().getCurrentAutomationPackageDirectory();
        if (dir == null) {
            return null;
        }
        return new AutomationPackageDescriptor(dir.toString(), model().getCurrentAutomationPackageName());
    }

    @POST
    @Path("ap/close")
    public void closeAP() {
        model().closeCurrentAutomationPackage();
    }

    /**
     * Executes the currently opened automation package on a remote Step controller and returns the executions this
     * started, one per plan unless the plans are wrapped into a single test set. Options left null in the request,
     * as well as an entirely absent request, fall back to what is configured in the CLI properties.
     */
    @Path("remote-execute")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<RemoteExecution> executeRemote(RemoteExecutionRequest request) {
        try {
            return model().executeRemote(request == null ? RemoteExecutionRequest.DEFAULTS : request);
        } catch (Exception e) {
            throw toServiceException(e);
        }
    }

    /**
     * Deploys the currently opened automation package to a remote Step controller. Options left null in the
     * request, as well as an entirely absent request, fall back to what is configured in the CLI properties.
     */
    @Path("remote-deploy")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AutomationPackageUpdateResult deployRemote(RemoteDeploymentRequest request) {
        try {
            return model().deployRemote(request == null ? RemoteDeploymentRequest.DEFAULTS : request);
        } catch (Exception e) {
            throw toServiceException(e);
        }
    }

    /**
     * Returns the options the two endpoints above fall back to, so that a client can display them and only
     * has to send back the options it actually overrides.
     */
    @Path("remote-defaults")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RemoteDefaults getRemoteDefaults() {
        try {
            return model().remoteDefaults();
        } catch (Exception e) {
            throw toServiceException(e);
        }
    }

    /**
     * Reports a misconfigured or incomplete request as a bad request, and anything else as an internal error.
     */
    private static ControllerServiceException toServiceException(Exception e) {
        boolean badRequest = e instanceof InvalidRequestException
            || e instanceof IllegalArgumentException
            || e instanceof IllegalStateException;
        if (!badRequest) {
            logger.error("Error while processing IDE request", e);
        }
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return error(message, badRequest ? Response.Status.BAD_REQUEST : Response.Status.INTERNAL_SERVER_ERROR, e);
    }

}
