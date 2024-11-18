package step.core.artefacts.reports.resolvedplan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.handlers.ArtefactHandlerManager;
import step.core.artefacts.handlers.ArtefactPathHelper;
import step.core.artefacts.reports.ParentSource;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;

import java.util.List;
import java.util.Map;

public class ResolvedPlanBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ResolvedPlanBuilder.class);

    private final ResolvedPlanNodeAccessor resolvedPlanNodeAccessor;
    private final DynamicBeanResolver dynamicBeanResolver;
    private final ArtefactHandlerManager artefactHandlerManager;
    private final ArtefactHashGenerator artefactHashGenerator;

    public ResolvedPlanBuilder(ExecutionContext executionContext) {
        resolvedPlanNodeAccessor = executionContext.require(ResolvedPlanNodeAccessor.class);
        artefactHandlerManager = executionContext.getArtefactHandlerManager();
        dynamicBeanResolver = executionContext.getDynamicBeanResolver();
        artefactHashGenerator = new ArtefactHashGenerator();
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
