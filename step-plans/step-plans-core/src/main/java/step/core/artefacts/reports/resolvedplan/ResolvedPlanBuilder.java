package step.core.artefacts.reports.resolvedplan;

import org.bson.types.ObjectId;
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
import step.core.execution.ReportNodeCache;
import step.core.plans.Plan;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
        return buildTreeRecursively(null, plan.getRoot(), null, plan, ParentSource.MAIN ,1);
    }

    private ResolvedPlanNode buildTreeRecursively(String parentId, AbstractArtefact artefactNode, String currentArtefactPath, Plan plan, ParentSource parentSource, int position) {
        String artefactHash = ArtefactPathHelper.generateArtefactHash(currentArtefactPath, artefactNode);

        // Create a clone of the artefact instance and remove the children
        AbstractArtefact artefactClone = dynamicBeanResolver.cloneDynamicValues(artefactNode);
        artefactClone.setChildren(null);
        ResolvedPlanNode resolvedPlanNode = new ResolvedPlanNode(artefactClone, artefactHash, parentId, parentSource, position);
        resolvedPlanNodeAccessor.save(resolvedPlanNode);
        resolvedPlanCache.put(resolvedPlanNode.artefactHash, resolvedPlanNode.getId().toHexString());

        // It is the responsibility of the handler to return relevant artefact node by source in the expected execution order
        ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler = artefactHandlerManager.getArtefactHandler(artefactNode);
        List<ResolvedChildren> resolvedChildrenBySource = artefactHandler.resolveChildrenArtefactBySource(artefactNode, currentArtefactPath);

        AtomicInteger localPosition = new AtomicInteger(0);
        // Recursively call children artefacts
        resolvedChildrenBySource.forEach(resolvedChildren -> {
            //filter node that were already processed to avoid stack overflow for plans calling themselves
            resolvedChildren.children.stream().filter(c -> (currentArtefactPath == null || !currentArtefactPath.contains(resolvedChildren.artefactPath)))
                    .forEach(child -> buildTreeRecursively(resolvedPlanNode.getId().toString(), child, resolvedChildren.artefactPath, plan, resolvedChildren.parentSource, localPosition.incrementAndGet()));
        });
        return resolvedPlanNode;
    }

    /**
     * Check first if it is a plan artefact, or a work artefact which are implicitly created at runtime (iterations, implicit session...)
     * For work artifact, verify whether the artefact hash is known, otherwise create a new resolved plan node and attach it to the closest known parent
     * This may only occur for dynamic call of keywords and plans that can only be resolved and selected at runtime, therefore, the child is introduced at the first position
     *
     * @param <ARTEFACT>       any class extending AbstractArtefact
     * @param artefactHash     the hash of the current artefact to be checked and added if missing
     * @param artefact         the artefact referenced by the artefact hash
     * @param parentReportNode the parent report node (might not correspond to an actual plan node
     * @param reportNodeCache  the report node cache of the execution used to retrieve the closest parent plan's node
     * @param parentSource
     */
    public <ARTEFACT extends AbstractArtefact> void checkAndAddMissingResolvedPlanNode(String artefactHash, ARTEFACT artefact, ReportNode parentReportNode, ReportNodeCache reportNodeCache, ParentSource parentSource) {
        if (!artefact.isWorkArtefact()) {
            resolvedPlanCache.computeIfAbsent(artefactHash, n -> {
                AbstractArtefact artefactClone = dynamicBeanResolver.cloneDynamicValues(artefact);
                String parentPlanNodeId = findClosestParentNodeId(parentReportNode, reportNodeCache);
                ResolvedPlanNode resolvedPlanNode = new ResolvedPlanNode(artefactClone, artefactHash, parentPlanNodeId, parentSource, 0);
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
        if (planNodeId == null) {
            throw new RuntimeException("No resolved plan node ancestor found for report node  " + parentReportNode.getId());
        }
        return planNodeId;
    }
}
