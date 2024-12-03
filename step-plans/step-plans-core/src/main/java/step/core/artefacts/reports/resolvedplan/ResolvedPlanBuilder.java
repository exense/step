package step.core.artefacts.reports.resolvedplan;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.handlers.ArtefactHandlerManager;
import step.core.artefacts.handlers.ArtefactPathHelper;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.execution.ExecutionContext;
import step.core.execution.ReportNodeCache;
import step.core.plans.Plan;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResolvedPlanBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ResolvedPlanBuilder.class);

    private final ResolvedPlanNodeAccessor resolvedPlanNodeAccessor;
    private final DynamicBeanResolver dynamicBeanResolver;
    private final ArtefactHandlerManager artefactHandlerManager;
    private final Map<String, String> resolvedPlanCache;

    public ResolvedPlanBuilder(ExecutionContext executionContext) {
        resolvedPlanNodeAccessor = executionContext.require(ResolvedPlanNodeAccessor.class);
        artefactHandlerManager = executionContext.getArtefactHandlerManager();
        dynamicBeanResolver = executionContext.getDynamicBeanResolver();
        resolvedPlanCache = new ConcurrentHashMap<>();
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
        resolvedPlanCache.put(resolvedPlanNode.artefactHash, resolvedPlanNode.getId().toHexString());

        // Resolve children, including children of called sub plans for plan and composite keyword
        // Starting with sub plans
        if (artefactNode.isCallingArtefactsFromOtherPlans()) {
            ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler = artefactHandlerManager.getArtefactHandler(artefactNode);
            try {
                String artefactId = ArtefactPathHelper.getArtefactId(artefactNode);
                if(currentArtefactPath != null && currentArtefactPath.contains(artefactId)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Detected recursive call to artefact '{}' in plan '{}'. Skipping initial creation of resolved plan for sub nodes.", artefactId, plan.getId().toString());
                    }
                    // TODO we should resume the creation of resolved plan at execution
                } else {
                    // This kind of artefact push a new path when executed for the underlying plans and his own children, so the currentArtefactPath get updated
                    currentArtefactPath = ArtefactPathHelper.getPathOfArtefact(currentArtefactPath, artefactId);
                    AbstractArtefact referencedChildArtefact = artefactHandler.resolveArtefactCall(artefactNode);
                    if(referencedChildArtefact != null) {
                        // Recursively call the referencedArtefact
                        buildTreeRecursively(resolvedPlanNode.getId().toString(), referencedChildArtefact, currentArtefactPath, plan);
                    }
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

    /**
     * Check first if it is a plan artefact, or a work artefact which are implicitly created at runtime (iterations, implicit session...)
     * For work artifact, verify whether the artefact hash is known, otherwise create a new resolved plan node and attach it to the closest known parent
     * @param artefactHash the hash of the current artefact to be checked and added if missing
     * @param artefact the artefact referenced by the artefact hash
     * @param parentReportNode  the parent report node (might not correspond to an actual plan node
     * @param reportNodeCache the report node cache of the execution used to retrieve the closest parent plan's node
     * @param <ARTEFACT> any class extending AbstractArtefact
     */
    public <ARTEFACT extends AbstractArtefact> void checkAndAddMissingResolvedPlanNode(String artefactHash, ARTEFACT artefact, ReportNode parentReportNode, ReportNodeCache reportNodeCache) {
        if (!artefact.isWorkArtefact()) {
            resolvedPlanCache.computeIfAbsent(artefactHash, n -> {
                AbstractArtefact artefactClone = dynamicBeanResolver.cloneDynamicValues(artefact);
                String parentPlanNodeId = findClosestParentNodeId(parentReportNode, reportNodeCache);
                ResolvedPlanNode resolvedPlanNode = new ResolvedPlanNode(artefactClone, artefactHash, parentPlanNodeId);
                resolvedPlanNodeAccessor.save(resolvedPlanNode);
                return resolvedPlanNode.getId().toHexString();
            });
        }
    }

    private String findClosestParentNodeId(ReportNode parentReportNode, ReportNodeCache reportNodeCache) {
        String planNodeId = resolvedPlanCache.get(parentReportNode.getArtefactHash());
        if (planNodeId == null) {
            ObjectId parentID = parentReportNode.getParentID();
            if (parentID != null) {
                planNodeId = findClosestParentNodeId(reportNodeCache.get(parentID), reportNodeCache);
            }
        }
        return planNodeId;
    }
}
