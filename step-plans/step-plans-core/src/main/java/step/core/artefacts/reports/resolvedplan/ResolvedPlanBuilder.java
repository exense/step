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

public class ResolvedPlanBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ResolvedPlanBuilder.class);

    private final ResolvedPlanNodeAccessor resolvedPlanNodeAccessor;
    private final DynamicBeanResolver dynamicBeanResolver;
    private final ArtefactHandlerManager artefactHandlerManager;

    public ResolvedPlanBuilder(ExecutionContext executionContext) {
        resolvedPlanNodeAccessor = executionContext.require(ResolvedPlanNodeAccessor.class);
        artefactHandlerManager = executionContext.getArtefactHandlerManager();
        dynamicBeanResolver = executionContext.getDynamicBeanResolver();
    }

    public ResolvedPlanNode buildResolvedPlan(Plan plan) {
        return buildTreeRecursively(null, plan.getRoot(), null, plan, ParentSource.MAIN);
    }

    private ResolvedPlanNode buildTreeRecursively(String parentId, AbstractArtefact artefactNode, final String currentArtefactPath, Plan plan, ParentSource parentSource) {
        String artefactHash = ArtefactPathHelper.generateArtefactHash(currentArtefactPath, artefactNode);

        // Create a clone of the artefact instance and remove the children
        AbstractArtefact artefactClone = dynamicBeanResolver.cloneDynamicValues(artefactNode);
        artefactClone.setChildren(null);
        ResolvedPlanNode resolvedPlanNode = new ResolvedPlanNode(artefactClone, artefactHash, parentId, parentSource);
        resolvedPlanNodeAccessor.save(resolvedPlanNode);

        // It is the responsibility of the handler to return relevant artefact node by source in the expected execution order
        ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler = artefactHandlerManager.getArtefactHandler(artefactNode);
        List<ResolvedChildren> resolvedChildrenBySource = artefactHandler.resolveChildrenArtefactBySource(artefactNode, currentArtefactPath);

        // Recursively call children artefacts
        resolvedChildrenBySource.forEach(resolvedChildren -> {
            //filter node that were already processed to avoid stack overflow for plans calling themselves
            resolvedChildren.children.stream().filter(c -> (currentArtefactPath == null || !ArtefactPathHelper.getArtefactId(c).contains(currentArtefactPath)))
                    .forEach(child -> buildTreeRecursively(resolvedPlanNode.getId().toString(), child, resolvedChildren.artefactPath, plan, resolvedChildren.parentSource));
        });
        return resolvedPlanNode;
    }
}
