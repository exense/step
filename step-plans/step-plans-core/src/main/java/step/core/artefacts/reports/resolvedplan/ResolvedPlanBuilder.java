package step.core.artefacts.reports.resolvedplan;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.handlers.ArtefactHandlerManager;
import step.core.artefacts.handlers.ArtefactHandlerRegistry;
import step.core.artefacts.handlers.ArtefactHashGenerator;
import step.core.artefacts.reports.ParentSource;
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
    private final ArtefactHandlerManager artefactHandlerManager;
    private final ExecutionContext executionContext;

    public ResolvedPlanBuilder(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        artefactHandlerManager = executionContext.getArtefactHandlerManager();
        planAccessor = executionContext.getPlanAccessor();
        resolvedPlanNodeAccessor = executionContext.require(ResolvedPlanNodeAccessor.class);
        // functionAccessor can be null
        functionAccessor = executionContext.get(FunctionAccessor.class);
        artefactHandlerRegistry = executionContext.getArtefactHandlerRegistry();
        dynamicBeanResolver = executionContext.getDynamicBeanResolver();
        dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(executionContext.getExpressionHandler()));
        artefactHashGenerator = new ArtefactHashGenerator();
        objectPredicate = executionContext.getObjectPredicate();
    }

    public ResolvedPlanNode buildResolvedPlan(Plan plan, Map<String, Object> bindings) {
        return buildTreeRecursively(null, plan.getRoot(), null, objectPredicate, bindings, ParentSource.MAIN);
    }

    private ResolvedPlanNode buildTreeRecursively(String parentId, AbstractArtefact artefactNode, String artefactPath, ObjectPredicate objectPredicate, Map<String, Object> bindings, ParentSource parentSource) {
        String artefactId = artefactNode.getId().toString();
        String artefactHash = artefactHashGenerator.generateArtefactHash(artefactPath, artefactId);

        // Create a clone of the artefact instance and remove the children
        AbstractArtefact artefactClone = dynamicBeanResolver.cloneDynamicValues(artefactNode);
        artefactClone.setChildren(null);
        ResolvedPlanNode resolvedPlanNode = new ResolvedPlanNode(artefactClone, artefactHash, parentId, parentSource);
        resolvedPlanNodeAccessor.save(resolvedPlanNode);

        // It is the responsibility of the handler to return relevant artefact node by source in the expected execution order
        ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler = artefactHandlerManager.getArtefactHandler(artefactNode);
        List<ResolvedChildren> resolvedChildrenBySource = artefactHandler.resolveChildrenArtefactBySource(artefactNode, artefactPath);

        resolvedChildrenBySource.forEach(resolvedChildren -> {
            resolvedChildren.children.forEach(child -> buildTreeRecursively(resolvedPlanNode.getId().toString(), child, resolvedChildren.artefactPath, objectPredicate, bindings, resolvedChildren.parentSource));
        });
        return resolvedPlanNode;
    }
}
