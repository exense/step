package step.core.references;

import io.swagger.v3.oas.annotations.tags.Tag;
import step.core.deployment.AbstractStepServices;
import step.core.objectenricher.ObjectHookRegistry;
import step.framework.server.security.Secured;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.*;

@Singleton
@Path("references")
@Tag(name = "References")
public class ReferenceFinderServices extends AbstractStepServices {

    private ReferenceFinder referenceFinder;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        referenceFinder = new ReferenceFinder(getContext().getEntityManager(), getContext().require(ObjectHookRegistry.class));
    }

    @Path("/findReferences")
    @POST
    @Secured
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<FindReferencesResponse> findReferences(FindReferencesRequest request) {
        return referenceFinder.findReferences(request);
    }
}
