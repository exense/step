package step.ide.api;

import io.swagger.v3.oas.annotations.Operation;
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
import step.core.deployment.AbstractStepServices;
import step.core.deployment.ControllerServiceException;
import step.ide.LocalIDEState;
import step.ide.exceptions.FileExistsException;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
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
        var ideState = LocalIDEState.get();

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
                LocalIDEState.get().validateInitializableAutomationPackageDirectory(path, false);
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
            LocalIDEState.get().useNewAutomationPackageDirectory(path, apName);
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
        var dir = LocalIDEState.get().getCurrentAutomationPackageDirectory();
        if (dir == null) {
            return null;
        }
        return new AutomationPackageDescriptor(dir.toString(), LocalIDEState.get().getCurrentAutomationPackageName());
    }

    @POST
    @Path("ap/close")
    public void closeAP() {
        LocalIDEState.get().closeCurrentAutomationPackage();
    }

    public record ProposeDirectoryResponse(String directory, List<String> warnings, List<String> errors) {
    }

    @Operation(description = "Takes an existing parent directory and desired AP name, and returns the proposed corresponding directory name, along with potential warnings or errors.")
    @GET
    @Path("ap/propose-directory")
    @Produces(MediaType.APPLICATION_JSON)
    public ProposeDirectoryResponse proposeAPDirectory(@QueryParam("existingParentDirectory") String existingParentDirectory, @QueryParam("apName") String apName) {

        if (existingParentDirectory == null || existingParentDirectory.isBlank()) {
            throw error("existingParentDirectory must not be empty", Response.Status.BAD_REQUEST);
        }
        if (apName == null || apName.isBlank()) {
            throw error("apName must not be empty", Response.Status.BAD_REQUEST);
        }

        java.nio.file.Path parentDirectory = java.nio.file.Path.of(existingParentDirectory);
        if (!Files.exists(parentDirectory)) {
            throw error("Parent directory does not exist: " + existingParentDirectory, Response.Status.BAD_REQUEST);
        }
        if (!Files.isDirectory(parentDirectory)) {
            throw error("Specified parent path is not a directory: " + existingParentDirectory, Response.Status.BAD_REQUEST);
        }

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        String sanitizedDirectoryName = sanitizeName(apName, warnings);
        java.nio.file.Path targetDirectory = parentDirectory.resolve(sanitizedDirectoryName);

        if (Files.exists(targetDirectory)) {
            try {
                if (!Files.isDirectory(targetDirectory)) {
                    errors.add("Target path already exists but is a file, not a directory: " + targetDirectory.toAbsolutePath());
                } else if (isDirectoryEmpty(targetDirectory)) { // this is what can potentially throw IOException
                    warnings.add("Directory already exists and is empty: " + targetDirectory.toAbsolutePath());
                } else {
                    warnings.add("Directory already exists and contains content: " + targetDirectory.toAbsolutePath());
                }
            } catch (IOException e) {
                throw error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        return new ProposeDirectoryResponse(targetDirectory.toAbsolutePath().toString(), warnings, errors);
    }

    /**
     * Replaces illegal cross-platform filesystem characters and reports modifications in the warnings list.
     */
    private String sanitizeName(String rawName, List<String> warnings) {
        String sanitized = rawName.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_").trim();

        if (!sanitized.equals(rawName)) {
            warnings.add("Directory name was sanitized from '" + rawName + "' to '" + sanitized + "'");
        }

        return sanitized;
    }

    private boolean isDirectoryEmpty(java.nio.file.Path directory) throws IOException {
        try (var entries = Files.list(directory)) {
            return entries.findFirst().isEmpty();
        }
    }

}
