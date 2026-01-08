package step.core.references;

import step.core.accessors.AbstractOrganizableObject;
import step.core.entities.EntityConstants;
import step.core.entities.EntityDependencyTreeVisitor;
import step.core.entities.EntityManager;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.resources.Resource;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReferenceFinder {

    private final EntityManager entityManager;

    public ReferenceFinder(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<FindReferencesResponse> findReferences(FindReferencesRequest request) {
        if (request.searchType == null) {
            throw new IllegalArgumentException("A valid searchType must be provided");
        }
        if (request.searchValue == null || request.searchValue.trim().isEmpty()) {
            throw new IllegalArgumentException("A non-empty searchValue must be provided");
        }

        List<FindReferencesResponse> results = new ArrayList<>();

        PlanAccessor planAccessor = (PlanAccessor) entityManager.getEntityByName(EntityConstants.plans).getAccessor();

        // Find composite keywords containing requested usages; composite KWs are really just plans in disguise :-)
        FunctionAccessor functionAccessor = (FunctionAccessor) entityManager.getEntityByName(EntityConstants.functions).getAccessor();

        try (Stream<Function> functionStream = functionAccessor.streamLazy()) {
            functionStream.forEach(function -> {
                List<Object> matchingObjects = getReferencedObjectsMatchingRequest(EntityConstants.functions, function, request);
                if (!matchingObjects.isEmpty()) {
                    results.add(new FindReferencesResponse(function));
                }
            });
        }

        // Find plans containing usages
        try (Stream<Plan> stream = (request.includeHiddenPlans) ? planAccessor.streamLazy() : planAccessor.getVisiblePlans()) {
            stream.forEach(plan -> {
                List<Object> matchingObjects = getReferencedObjectsMatchingRequest(EntityConstants.plans, plan, request);
                if (!matchingObjects.isEmpty()) {
                    results.add(new FindReferencesResponse(plan));
                }
            });
        }

        // Sort the results by name
        results.sort(Comparator.comparing(f -> f.name));
        return results;
    }

    private List<Object> getReferencedObjectsMatchingRequest(String entityType, AbstractOrganizableObject object, FindReferencesRequest request) {
        List<Object> referencedObjects = getReferencedObjects(entityType, object).stream().filter(o -> (o != null &&  !o.equals(object))).collect(Collectors.toList());
        //System.err.println("objects referenced from plan: " + planToString(plan) + ": "+ referencedObjects.stream().map(ReferenceFinderServices::objectToString).collect(Collectors.toList()));
        return referencedObjects.stream().filter(o -> doesRequestMatch(request, o)).collect(Collectors.toList());
    }

    // returns a (generic) set of objects referenced by a plan
    private Set<Object> getReferencedObjects(String entityType, AbstractOrganizableObject object) {
        Set<Object> referencedObjects = new HashSet<>();

        // The references can be filled in three different ways due to the implementation:
        // 1. through the predicate (just below)
        // 2. by (actual object) reference in the tree visitor (onResolvedEntity)

        // No context predicate is used by the reference finder, since we want to find all entities (i.e. if we search the usages of a  Keyword from the Common project, we should be able
        // to find plans using it in other projects.
        // This unfortunately can return incorrect results, i.e. a keyword "MyKeyword" is created in ProjectA and ProjectB, A PlanA is created in ProjectA and is using the KW of the same project.
        // Searching usage of "MyKeyword" in projectB will return the planA from projectA
        EntityDependencyTreeVisitor entityDependencyTreeVisitor = new EntityDependencyTreeVisitor(entityManager, o -> true);
        FindReferencesTreeVisitor entityTreeVisitor = new FindReferencesTreeVisitor(entityManager, referencedObjects);
        entityDependencyTreeVisitor.visitEntityDependencyTree(entityType, object.getId().toString(), entityTreeVisitor, EntityDependencyTreeVisitor.VISIT_MODE.RESOLVE_ALL);

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
        } else if (o instanceof Resource) {
            Resource r = (Resource) o;
            switch (req.searchType) {
                case RESOURCE_NAME:
                    return r.getAttribute(AbstractOrganizableObject.NAME).equals(req.searchValue);
                case RESOURCE_ID:
                    return r.getId().toString().equals(req.searchValue);
                default:
                    return false;
            }
        } else {
            return false;
        }
    }
}
