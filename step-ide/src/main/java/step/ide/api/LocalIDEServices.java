package step.ide.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.deployment.AbstractStepServices;
import step.ide.LocalIDEState;

import java.io.File;
import java.util.Objects;

@Path("/local/ide")
@Tag(name = "IDE")
public class LocalIDEServices extends AbstractStepServices {

    private static final Logger logger = LoggerFactory.getLogger(LocalIDEServices.class);

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext globalContext = getContext();
    }

    @POST
    @Path("ap/useExisting")
    @Consumes(MediaType.APPLICATION_JSON)
    public void useExistingAP(@QueryParam("directory") String directory) {
        File file = new File(Objects.requireNonNull(directory, "directory must not be null"));
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
        File file = new File(Objects.requireNonNull(existingEmptyDirectory, "existingEmptyDirectory must not be null"));
        Objects.requireNonNull(apName, "apName must not be null");
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

}
