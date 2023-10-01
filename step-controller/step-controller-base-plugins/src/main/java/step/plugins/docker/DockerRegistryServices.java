package step.plugins.docker;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.controller.services.entities.AbstractEntityServices;
import step.core.GlobalContext;
import step.core.docker.DockerRegistryConfiguration;
import step.core.docker.DockerRegistryConfigurationAccessor;
import step.framework.server.access.AuthorizationManager;
import step.framework.server.security.Secured;
import step.framework.server.security.SecuredContext;

import java.util.ArrayList;
import java.util.List;

@Singleton
@Path("docker/registry")
@Tag(name = "Docker Registries")
@Tag(name = "Entity=Docker Registry")
@SecuredContext(key = "entity", value = "dockerRegistries")
public class DockerRegistryServices extends AbstractEntityServices<DockerRegistryConfiguration> {

    protected DockerRegistryConfigurationAccessor dockerRegistryConfigurationAccessor;

    public static final Logger logger = LoggerFactory.getLogger(DockerRegistryServices.class);

    public DockerRegistryServices() {
        super(DockerRegistryControllerPlugin.ENTITY_DOCKER_REGISTRIES);
    }


    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        dockerRegistryConfigurationAccessor = context.get(DockerRegistryConfigurationAccessor.class);
    }

    @Operation(operationId = "list{Entity}", description = "Retrieves all the entities")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    @Secured(right = "{entity}-read")
    @SuppressWarnings("unused")
    public List<DockerRegistryConfiguration> getDockerRegistryConfigurations() {
        List<DockerRegistryConfiguration> result = new ArrayList<>();
        dockerRegistryConfigurationAccessor.getAll().forEachRemaining(result::add);
        return result;
    }
}
