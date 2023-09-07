package step.plugins.docker;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.deployment.AbstractStepServices;

import java.util.ArrayList;
import java.util.List;

@Singleton
@Path("docker")
@Tag(name = "Docker Registries")
public class DockerRegistryServices extends AbstractStepServices {
    protected DockerRegistryConfigurationAccessor dockerRegistryConfigurationAccessor;

    public static final Logger logger = LoggerFactory.getLogger(DockerRegistryServices.class);


    @PostConstruct
    public void init() throws Exception {
        super.init();
        dockerRegistryConfigurationAccessor = getContext().get(DockerRegistryConfigurationAccessor.class);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/registry")
    public void saveDockerRegistryConfiguration(DockerRegistryConfiguration registryConfiguration) {
        dockerRegistryConfigurationAccessor.save(registryConfiguration);
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/registry/{id}")
    public DockerRegistryConfiguration getDockerRegistryConfiguration(@PathParam("id") String id) {
        return dockerRegistryConfigurationAccessor.get(id);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/registry/{id}")
    public void deleteDockerRegistryConfiguration(@PathParam("id") String id) {
        dockerRegistryConfigurationAccessor.remove(new ObjectId(id));
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/registry/list")
    public List<DockerRegistryConfiguration> getDockerRegistryConfigurations() {
        List<DockerRegistryConfiguration> result = new ArrayList<>();
        dockerRegistryConfigurationAccessor.getAll().forEachRemaining(result::add);
        return result;
    }
}
