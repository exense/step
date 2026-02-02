package step.core.references;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.AbstractContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.entities.EntityConstants;
import step.core.entities.EntityDependencyTreeVisitor;
import step.core.entities.EntityManager;
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.resources.Resource;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReferenceFinder {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceFinder.class);

    private final EntityManager entityManager;
    private final ObjectHookRegistry objectHookRegistry;

    public ReferenceFinder(EntityManager entityManager, ObjectHookRegistry objectHookRegistry) {
        this.entityManager = entityManager;
        this.objectHookRegistry = objectHookRegistry;
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
        return getReferencedObjects(entityType, object, request.searchValue).stream()
                .filter(o -> (o != null &&  !o.equals(object)))
                .filter(o -> doesRequestMatch(request, o))
                .collect(Collectors.toList());
    }

    // returns a (generic) set of objects referenced by a plan
    private Set<Object> getReferencedObjects(String entityType, AbstractOrganizableObject object, String searchValue) {
        Set<Object> referencedObjects = new HashSet<>();

        // The references can be filled in two different ways due to the implementation:
        // 1. by (actual object) reference in the tree visitor (onResolvedEntity)
        // 2. by object ID in the tree visitor (onResolvedEntityId)

        // When searching the references of a give entity we must apply the predicate as if we were in the context of this entity
        ObjectPredicate predicate = o -> true; //default value for non enricheable objects
        if (object instanceof EnricheableObject) {
            AbstractContext context = new AbstractContext() {};
            try {
                objectHookRegistry.rebuildContext(context, (EnricheableObject) object);
            } catch (Exception e) {
                //The getReferencedObjects method is invoked for all entities found in the system, for some entities (for example plans that belongs to a deleted project), the context cannot be rebuilt.
                //These expected errors are ignored
                logger.warn("Unable to inspect the {} with id {} while searching for usages of {}", entityType, object.getId(), searchValue, e);
                return referencedObjects;
            }
            predicate = objectHookRegistry.getObjectPredicate(context);
        }
        EntityDependencyTreeVisitor entityDependencyTreeVisitor = new EntityDependencyTreeVisitor(entityManager, predicate);
        FindReferencesTreeVisitor entityTreeVisitor = new FindReferencesTreeVisitor(entityManager, referencedObjects);
        entityDependencyTreeVisitor.visitEntityDependencyTree(entityType, object.getId().toString(), entityTreeVisitor, EntityDependencyTreeVisitor.VISIT_MODE.RESOLVE_ALL);

        return referencedObjects;
    }

    private boolean doesRequestMatch(FindReferencesRequest req, Object o) {
        if (o instanceof Plan) {
            Plan p = (Plan) o;
            switch (req.searchType) {
                case PLAN_NAME:
                    return req.searchValue.equals(p.getAttribute(AbstractOrganizableObject.NAME));
                case PLAN_ID:
                    return p.getId().toString().equals(req.searchValue);
                default:
                    return false;
            }
        } else if (o instanceof Function) {
            Function f = (Function) o;
            switch (req.searchType) {
                case KEYWORD_NAME:
                    return req.searchValue.equals(f.getAttribute(AbstractOrganizableObject.NAME));
                case KEYWORD_ID:
                    return f.getId().toString().equals(req.searchValue);
                default:
                    return false;
            }
        } else if (o instanceof Resource) {
            Resource r = (Resource) o;
            switch (req.searchType) {
                case RESOURCE_NAME:
                    return req.searchValue.equals(r.getAttribute(AbstractOrganizableObject.NAME));
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
