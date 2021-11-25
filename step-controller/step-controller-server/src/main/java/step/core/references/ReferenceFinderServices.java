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
import step.functions.accessor.FunctionAccessor;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

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


    // Uncomment for easier debugging (poor man's Unit Test), URL will be http://localhost:8080/rest/references/findReferencesDebug
    /*
    @GET
    @Path("/findReferencesDebug")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FindReferencesResponse> findReferencesTest() {
        List<FindReferencesResponse> result = new ArrayList<>();
        result.addAll(findReferences(new FindReferencesRequest(PLAN_NAME, "TestXXX")));
//        result.addAll(findReferences(new FindReferencesRequest(PLAN_ID, "6195001c0a98d92da8a57830")));
        result.addAll(findReferences(new FindReferencesRequest(KEYWORD_NAME, "UnitTest")));
//        result.addAll(findReferences(new FindReferencesRequest(KEYWORD_ID, "60cca3488b81b227a5fe92d9")));
        return result;
    }
     //*/

    @Path("/findReferences")
    @POST
    @Secured
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<FindReferencesResponse> findReferences(FindReferencesRequest request) {
        if (request.searchType == null) {
            throw new IllegalArgumentException("A valid searchType must be provided");
        }
        if (request.searchValue == null || request.searchValue.trim().isEmpty()) {
            throw new IllegalArgumentException("A non-empty searchValue must be provided");
        }

        List<FindReferencesResponse> results = new ArrayList<>();

        PlanAccessor planAccessor = (PlanAccessor) entityManager.getEntityByName(EntityManager.plans).getAccessor();

        // Find plans containing usages
        List<Plan> plansToConsider = planAccessor.stream().filter(p -> request.includeEphemerals || !isEphemeral(p)).collect(Collectors.toList());

        plansToConsider.forEach(plan -> {
            List<Object> matchingObjects = getReferencedObjectsMatchingRequest(plan, request);
            if (!matchingObjects.isEmpty()) {
                results.add(new FindReferencesResponse(plan));
            }
        });

        // Find composite keywords containing requested usages; composite KWs are really just plans in disguise :-)
        FunctionAccessor functionAccessor = (FunctionAccessor) entityManager.getEntityByName(EntityManager.functions).getAccessor();

        // we don't have access to the composite functions plugin from here, so we use class names and reflection.
        List<Function> compositeFunctions = functionAccessor.stream().filter(f -> f.getClass().getName().equals("step.plugins.functions.types.CompositeFunction")).collect(Collectors.toList());
        compositeFunctions.forEach(function -> {
            String planId = getPlanIdForCompositeFunction(function);
            if (planId != null) {
                Plan plan = planAccessor.get(planId);
                if (plan != null) {
                    List<Object> matchingObjects = getReferencedObjectsMatchingRequest(plan, request);
                    if (!matchingObjects.isEmpty()) {
                        results.add(new FindReferencesResponse(function));
                    }
                }
            }
        });

        // Sort the results by name
        results.sort(Comparator.comparing(f -> f.name));
        return results;
    }

    private String getPlanIdForCompositeFunction(Function composite) {
        try {
            Method m = composite.getClass().getMethod("getPlanId");
            return (String) m.invoke(composite);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Object> getReferencedObjectsMatchingRequest(Plan plan, FindReferencesRequest request) {
        List<Object> referencedObjects = getReferencedObjects(plan).stream().filter(o -> !o.equals(plan)).collect(Collectors.toList());
        //System.err.println("objects referenced from plan: " + planToString(plan) + ": "+ referencedObjects.stream().map(ReferenceFinderServices::objectToString).collect(Collectors.toList()));
        return referencedObjects.stream().filter(o -> doesRequestMatch(request, o)).collect(Collectors.toList());
    }


    private boolean isEphemeral(Plan plan) {
        return !plan.hasAttribute("project");
    }

    // returns a (generic) set of objects referenced by a plan
    private Set<Object> getReferencedObjects(Plan plan) {
        Set<Object> referencedObjects = new HashSet<>();

        // The references can be filled in three different ways due to the implementation:
        // 1. through the predicate (just below)
        // 2. by (actual object) reference in the tree visitor (onResolvedEntity)
        // 3. by object ID in the tree visitor (onResolvedEntityId)

        ObjectPredicate visitedObjectPredicate = visitedObject -> {
            referencedObjects.add(visitedObject);
            return true;
        };

        EntityDependencyTreeVisitor entityDependencyTreeVisitor = new EntityDependencyTreeVisitor(entityManager, visitedObjectPredicate);
        FindReferencesTreeVisitor entityTreeVisitor = new FindReferencesTreeVisitor(entityManager, referencedObjects);
        entityDependencyTreeVisitor.visitEntityDependencyTree(EntityManager.plans, plan.getId().toString(), entityTreeVisitor, false);

        return referencedObjects;
    }

    private boolean doesRequestMatch(FindReferencesRequest req, Object o) {
        if (o instanceof Plan) {
            Plan p = (Plan) o;
            switch (req.searchType) {
                case PLAN_NAME:
                    return p.getAttribute(AbstractOrganizableObject.NAME).equals(req.searchValue);
                case PLAN_ID:
                    return p.getId().toString().equals(req.searchValue);
                default:
                    return false;
            }
        } else if (o instanceof Function) {
            Function f = (Function) o;
            switch (req.searchType) {
                case KEYWORD_NAME:
                    return f.getAttribute(AbstractOrganizableObject.NAME).equals(req.searchValue);
                case KEYWORD_ID:
                    return f.getId().toString().equals(req.searchValue);
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    // the following functions are only needed for debugging
    private static String objectToString(Object o) {
        if (o instanceof Plan) {
            return planToString((Plan) o);
        } else if (o instanceof Function) {
            return functionToString((Function) o);
        } else {
            return o.getClass() + " " + o.toString();
        }
    }

    private static String planToString(Plan plan) {
        return "PLAN: " + plan.getAttributes().toString() + " id=" + plan.getId().toString();
    }

    private static String functionToString(Function function) {
        return "FUNCTION: " + function.getAttributes().toString() + " id=" + function.getId().toString();
    }


}
