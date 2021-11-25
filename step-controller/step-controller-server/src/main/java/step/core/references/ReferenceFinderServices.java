package step.core.references;

import io.swagger.v3.oas.annotations.tags.Tag;
import step.core.accessors.AbstractOrganizableObject;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.entities.EntityDependencyTreeVisitor;
import step.core.entities.EntityManager;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.functions.Function;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

import static step.core.references.FindReferencesRequest.Type.*;

@Singleton
@Path("references")
@Tag(name = "References")
public class ReferenceFinderServices extends AbstractServices {

    private EntityManager entityManager;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        entityManager = getContext().getEntityManager();
    }


    @Path("/findReferences")
    @POST
    @Secured
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<FindReferencesResponse> findReferences(FindReferencesRequest request) {
        if (request.referenceType == null) {
            throw new IllegalArgumentException("A valid referenceType must be provided");
        }
        if (request.value == null || request.value.trim().isEmpty()) {
            throw new IllegalArgumentException("A non-empty value must be provided");
        }

        List<FindReferencesResponse> results = new ArrayList<>();

        PlanAccessor planAccessor = (PlanAccessor) entityManager.getEntityByName("plans").getAccessor();
        List<Plan> plansToConsider = planAccessor.stream().filter(p -> request.includeEphemerals || !isEphemeral(p)).collect(Collectors.toList());

        plansToConsider.forEach(plan -> {

            List<Object> referencedObjects = getReferencedObjects(plan).stream().filter(o -> !o.equals(plan)).collect(Collectors.toList());
            //System.err.println("objects referenced from plan: " + planToString(plan) + ": "+ referencedObjects.stream().map(ReferenceFinderServices::objectToString).collect(Collectors.toList()));
            List<Object> matchingObjects = referencedObjects.stream().filter(o -> doesRequestMatch(request, o)).collect(Collectors.toList());

            if (!matchingObjects.isEmpty()) {
                results.add(new FindReferencesResponse(plan));
            }
        });

        // Sort the results by name
        results.sort(Comparator.comparing(f -> f.name));
        return results;
    }

    private boolean isEphemeral(Plan plan) {
        return !plan.hasAttribute("project");
    }

    // returns a (generic) set of objects referenced by a plan
    private Set<Object> getReferencedObjects(Plan plan) {
        Set<Object> referencedObjects = new HashSet<>();

        ObjectPredicate visitedObjectPredicate = visitedObject -> {
            referencedObjects.add(visitedObject);
            return true;
        };

        EntityDependencyTreeVisitor entityDependencyTreeVisitor = new EntityDependencyTreeVisitor(entityManager, visitedObjectPredicate);
        FindReferencesTreeVisitor entityTreeVisitor = new FindReferencesTreeVisitor(referencedObjects);
        entityDependencyTreeVisitor.visitEntityDependencyTree("plans", plan.getId().toString(), entityTreeVisitor, false);

        return referencedObjects;
    }

    private boolean doesRequestMatch(FindReferencesRequest req, Object o) {
        if (o instanceof Plan) {
            Plan p = (Plan) o;
            switch (req.referenceType) {
                case PLAN_NAME:
                    return p.getAttribute(AbstractOrganizableObject.NAME).equals(req.value);
                case PLAN_ID:
                    return p.getId().toString().equals(req.value);
                default:
                    return false;
            }
        } else if (o instanceof Function) {
            Function f = (Function) o;
            switch (req.referenceType) {
                case KEYWORD_NAME:
                    return f.getAttribute(AbstractOrganizableObject.NAME).equals(req.value);
                case KEYWORD_ID:
                    return f.getId().toString().equals(req.value);
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    // the following functions are only needed for debugging
    private static String planToString(Plan plan) {
        return "PLAN: " + plan.getAttributes().toString() + " id=" + plan.getId().toString();
    }

    private static String functionToString(Function function) {
        return "FUNCTION: " + function.getAttributes().toString() + " id=" + function.getId().toString();
    }


    private static String objectToString(Object o) {
        if (o instanceof Plan) {
            return planToString((Plan) o);
        } else if (o instanceof Function) {
            return functionToString((Function) o);
        } else {
            return o.getClass() + " " + o.toString();
        }
    }
}
