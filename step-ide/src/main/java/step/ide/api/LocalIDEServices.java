package step.ide.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.function.Failable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.deployment.AbstractStepServices;
import step.ide.LocalIDEState;

import java.io.File;

@Path("/local/ide")
@Tag(name = "IDE")
public class LocalIDEServices extends AbstractStepServices {

    private static final Logger logger = LoggerFactory.getLogger(LocalIDEServices.class);

    @PostConstruct
    public void init() throws Exception {
        super.init();
    }

    @POST
    @Path("ap/useExisting")
    @Consumes(MediaType.APPLICATION_JSON)
    public void useExistingAP(@QueryParam("directory") String directory) {
        if (directory == null || directory.isBlank()) {
            throw new WebApplicationException("directory must not be empty", Response.Status.BAD_REQUEST);
        }
        File file = new File(directory);
        if (!file.isDirectory() || !file.canRead()) {
            throw new WebApplicationException("Not a readable directory: " + directory, Response.Status.BAD_REQUEST);
        }
        try {
            LocalIDEState.get().useExistingAutomationPackageDirectory(file);

        } catch (Exception e) {
            logger.error("Unable to use existing AP directory: {}", directory, e);
            throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("ap/initializeNew")
    @Consumes(MediaType.APPLICATION_JSON)
    public void initializeNewAP(@QueryParam("existingEmptyDirectory") String existingEmptyDirectory, @QueryParam("apName") String apName) {
        if (existingEmptyDirectory == null || existingEmptyDirectory.isBlank()) {
            throw new WebApplicationException("existingEmptyDirectory is required", Response.Status.BAD_REQUEST);
        }
        if (apName == null || apName.isBlank()) {
            throw new WebApplicationException("apName is required", Response.Status.BAD_REQUEST);
        }
        File file = new File(existingEmptyDirectory);
        if (!file.isDirectory() || !file.canWrite()) {
            throw new WebApplicationException("Not a writable directory: " + existingEmptyDirectory, Response.Status.BAD_REQUEST);
        }
        try {
            LocalIDEState.get().useNewAutomationPackageDirectory(file, apName);
        } catch (Exception e) {
            logger.error("Unable to initialize new AP directory: {}", existingEmptyDirectory, e);
            throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("ap/current")
    @Produces(MediaType.APPLICATION_JSON)
    public String getCurrentAP() {
        File dir = LocalIDEState.get().getCurrentAutomationPackageDirectory();
        return Failable.get(() -> new ObjectMapper().writeValueAsString(dir));
    }

    @POST
    @Path("ap/close")
    public void closeAP() {
        LocalIDEState.get().closeCurrentAutomationPackage();
    }

}
