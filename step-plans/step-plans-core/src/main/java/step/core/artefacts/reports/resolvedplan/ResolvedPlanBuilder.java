package step.core.artefacts.reports.resolvedplan;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.handlers.ArtefactHandlerRegistry;
import step.core.artefacts.handlers.ArtefactHashGenerator;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.functions.accessor.FunctionAccessor;

import java.util.List;
import java.util.Map;

public class ResolvedPlanBuilder {

    private final PlanAccessor planAccessor;
    private final ResolvedPlanNodeAccessor resolvedPlanNodeAccessor;
    private final FunctionAccessor functionAccessor;
    private final ArtefactHashGenerator artefactHashGenerator;
    private final DynamicJsonObjectResolver dynamicJsonObjectResolver;
    private final ArtefactHandlerRegistry artefactHandlerRegistry;
    private final ObjectPredicate objectPredicate;
    private final DynamicBeanResolver dynamicBeanResolver;

    public ResolvedPlanBuilder(ExecutionContext executionContext) {
        planAccessor = executionContext.getPlanAccessor();
        resolvedPlanNodeAccessor = executionContext.require(ResolvedPlanNodeAccessor.class);
        functionAccessor = executionContext.require(FunctionAccessor.class);
        artefactHandlerRegistry = executionContext.getArtefactHandlerRegistry();
        dynamicBeanResolver = executionContext.getDynamicBeanResolver();
        dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(executionContext.getExpressionHandler()));
        artefactHashGenerator = new ArtefactHashGenerator();
        objectPredicate = executionContext.getObjectPredicate();
    }

    public ResolvedPlanNode buildResolvedPlan(Plan plan, Map<String, Object> bindings) {
        return buildTreeRecursively(null, plan.getRoot(), null, objectPredicate, bindings);
    }

    private ResolvedPlanNode buildTreeRecursively(String parentId, AbstractArtefact artefactNode, String artefactPath, ObjectPredicate objectPredicate, Map<String, Object> bindings) {
        String artefactId = artefactNode.getId().toString();
        String artefactHash = artefactHashGenerator.generateArtefactHash(artefactPath, artefactId);

        // Resolve children
        List<AbstractArtefact> childrenArtefacts;
        String newArtefactPath;
        if (artefactNode.isCallingArtefactsFromOtherPlans()) {
            ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler = artefactHandlerRegistry.getArtefactHandler((Class<AbstractArtefact>) artefactNode.getClass());
            AbstractArtefact referencedChildArtefact = artefactHandler.resolveArtefactCall(artefactNode, dynamicJsonObjectResolver, bindings, objectPredicate, planAccessor, functionAccessor);
            childrenArtefacts = List.of(referencedChildArtefact);
            newArtefactPath = ArtefactHashGenerator.getPath(artefactPath, artefactNode.getId().toString());
        } else {
            childrenArtefacts = artefactNode.getChildren();
            newArtefactPath = artefactPath;
        }

        // Create a clone of the artefact instance and remove the children
        AbstractArtefact artefactClone = dynamicBeanResolver.cloneDynamicValues(artefactNode);
        artefactClone.setChildren(null);
        ResolvedPlanNode resolvedPlanNode = new ResolvedPlanNode(artefactClone, artefactHash, parentId);
        resolvedPlanNodeAccessor.save(resolvedPlanNode);

        // Recursively call children artefacts
        for (AbstractArtefact child : childrenArtefacts) {
            buildTreeRecursively(resolvedPlanNode.getId().toString(), child, newArtefactPath, objectPredicate, bindings);
        }

        return resolvedPlanNode;
    }
}
