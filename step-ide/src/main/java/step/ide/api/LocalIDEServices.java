package step.ide.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.ApResourceBrowser;
import step.automation.packages.ApResourceContentResponse;
import step.automation.packages.ApResourceNotFoundException;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.ControllerServiceException;
import step.core.filebrowser.DirectoryListing;
import step.ide.LocalIDEState;
import step.ide.exceptions.FileExistsException;

import java.io.File;
import java.util.function.Function;

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

    // ---------------------------------------------------------------------------------------------
    // Browsing the automation package currently open in the editor
    //
    // Same picker as the controller's /automation-packages/ap-resources/browse, on a different data
    // source: an exploded directory rather than a deployed archive. Two differences follow from that,
    // and they are the reason this cannot be served by the controller service:
    //
    //  - only LocalIDEState knows which automation package is open and where it lives, so there is
    //    deliberately no apId and no root parameter here - the client cannot address another package;
    //  - the value produced for a picked file is the plain relative path, not an apResource: reference.
    //    The editor writes it straight into the YAML descriptor, which has no apId to name (one only
    //    exists once the package is deployed, and differs per deployment).
    // ---------------------------------------------------------------------------------------------

    /**
     * Lists the content of one directory of the automation package currently open in the editor.
     *
     * @param path      the AP-root relative path to open at, typically the value currently held by the
     *                  edited field. A directory is listed itself, a file lists its parent directory so
     *                  that the client can preselect it. Empty for the root of the package
     * @param filesOnly whether directories should be left out of the listing
     * @param dirsOnly  whether files should be left out of the listing
     */
    @GET
    @Path("ap/browse")
    @Produces(MediaType.APPLICATION_JSON)
    public DirectoryListing browseAP(@QueryParam("path") String path,
                                     @QueryParam("filesOnly") @DefaultValue("false") boolean filesOnly,
                                     @QueryParam("dirsOnly") @DefaultValue("false") boolean dirsOnly) {
        return browse(currentAutomationPackageDirectory(), path, filesOnly, dirsOnly);
    }

    /**
     * Downloads the content of a file of the automation package currently open in the editor.
     *
     * @param path   the AP-root relative path of the file
     * @param inline whether the content should be served as {@code inline} rather than as an attachment
     */
    @GET
    @Path("ap/content")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getAPContent(@QueryParam("path") String path,
                                 @QueryParam("inline") boolean inline) {
        return content(currentAutomationPackageDirectory(), path, inline);
    }

    /**
     * @return the directory of the automation package currently open in the editor
     * @throws ControllerServiceException 409 if no automation package is open. This is a state
     *                                    problem rather than a malformed request: the very same call
     *                                    succeeds once one has been opened
     */
    private static File currentAutomationPackageDirectory() {
        java.nio.file.Path directory = LocalIDEState.get().getCurrentAutomationPackageDirectory();
        if (directory == null) {
            throw error("No automation package is currently open in the editor", Response.Status.CONFLICT);
        }
        return directory.toFile();
    }

    /**
     * Package private, taking the automation package directory explicitly: it is the part of
     * {@link #browseAP} that does not depend on the {@link LocalIDEState} singleton.
     */
    static DirectoryListing browse(File apDirectory, String path, boolean filesOnly, boolean dirsOnly) {
        try {
            // identity: in the editor the relative path is itself the reference, see the note above
            return ApResourceBrowser.browse(apDirectory, path, Function.identity(),
                ApResourceBrowser.EntryFilter.of(filesOnly, dirsOnly));
        } catch (ApResourceNotFoundException e) {
            throw error(e.getMessage(), Response.Status.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            throw error(e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    /**
     * @see #browse(File, String, boolean, boolean)
     */
    static Response content(File apDirectory, String path, boolean inline) {
        if (path == null || path.isBlank()) {
            throw error("The path of the file to download must be provided", Response.Status.BAD_REQUEST);
        }
        try {
            return ApResourceContentResponse.of(ApResourceBrowser.openEntry(apDirectory, path), inline);
        } catch (ApResourceNotFoundException e) {
            throw error(e.getMessage(), Response.Status.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            throw error(e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

}
