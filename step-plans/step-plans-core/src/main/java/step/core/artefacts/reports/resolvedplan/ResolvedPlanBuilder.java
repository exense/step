package step.core.artefacts.reports.resolvedplan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.handlers.ArtefactHandlerManager;
import step.core.artefacts.handlers.ArtefactPathHelper;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;

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
        return buildTreeRecursively(null, plan.getRoot(), null, plan);
    }

    private ResolvedPlanNode buildTreeRecursively(String parentId, AbstractArtefact artefactNode, String currentArtefactPath, Plan plan) {
        String artefactHash = ArtefactPathHelper.generateArtefactHash(currentArtefactPath, artefactNode);

        // Create a clone of the artefact instance and remove the children
        AbstractArtefact artefactClone = dynamicBeanResolver.cloneDynamicValues(artefactNode);
        artefactClone.setChildren(null);
        ResolvedPlanNode resolvedPlanNode = new ResolvedPlanNode(artefactClone, artefactHash, parentId);
        resolvedPlanNodeAccessor.save(resolvedPlanNode);

        // Resolve children, including children of called sub plans for plan and composite keyword
        // Starting with sub plans
        if (artefactNode.isCallingArtefactsFromOtherPlans()) {
            ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler = artefactHandlerManager.getArtefactHandler(artefactNode);
            try {
                // This kind of artefact push a new path when executed for the underlying plans and his own children, so the currentArtefactPath get updated
                currentArtefactPath = ArtefactPathHelper.getPathOfArtefact(currentArtefactPath, artefactNode);
                AbstractArtefact referencedChildArtefact = artefactHandler.resolveArtefactCall(artefactNode);
                if(referencedChildArtefact != null) {
                    // Recursively call the referencedArtefact
                    buildTreeRecursively(resolvedPlanNode.getId().toString(), referencedChildArtefact, currentArtefactPath, plan);
                }
            } catch (Exception e) {
                String message = "Unable to resolve called plan or composite keyword in plan " + plan.getId();
                if (logger.isDebugEnabled()) {
                    logger.debug(message, e);
                } else {
                    logger.warn(message);
                }
            }
        }
        // Recursively call children artefacts
        for (AbstractArtefact child : artefactNode.getChildren()) {
            buildTreeRecursively(resolvedPlanNode.getId().toString(), child, currentArtefactPath, plan);
        }

        return resolvedPlanNode;
    }
}
