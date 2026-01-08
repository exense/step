package step.core.references;

import io.swagger.v3.oas.annotations.tags.Tag;
import step.core.accessors.AbstractOrganizableObject;
import step.core.deployment.AbstractStepServices;
import step.core.entities.EntityConstants;
import step.framework.server.security.Secured;
import step.core.entities.EntityDependencyTreeVisitor;
import step.core.entities.EntityManager;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.resources.Resource;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@Path("references")
@Tag(name = "References")
public class ReferenceFinderServices extends AbstractStepServices {

    private ReferenceFinder referenceFinder;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        referenceFinder = new ReferenceFinder(getContext().getEntityManager());
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
