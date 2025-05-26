package step.core.artefacts.reports.resolvedplan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.execution.model.Execution;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class ResolvedPlanNodeCachedAccessor  {

    private static final Logger logger = LoggerFactory.getLogger(ResolvedPlanNodeCachedAccessor.class);
    private final ResolvedPlanNodeAccessor underlyingAccessor;
    private final Map<String, List<ResolvedPlanNode>> cacheByParentId = new ConcurrentHashMap<>();

    public ResolvedPlanNodeCachedAccessor(ResolvedPlanNodeAccessor underlyingAccessor, Execution execution) {
        this.underlyingAccessor = underlyingAccessor;
        //To avoid migration of older execution in a migration task we migrate on the fly when required
        migrateDataIFRequired(execution);
        //preload the cache for the given execution id
        underlyingAccessor.getByExecutionId(execution.getId().toHexString()).forEach(this::store);
    }

    /** This class and the field executionId in ResolvedPlanNode were introduced to improve the performance
     * of the aggregated report tree builder. To avoid migration of all execution we migrate data on the fly when required
     * @param execution the execution for mich the resolved plan nodes might have to be migrated once
     */
    private void migrateDataIFRequired(Execution execution) {
        Optional.ofNullable(execution.getResolvedPlanRootNodeId()).map(this::getFromUnderlyingAccessor).ifPresent(r -> {
            if (r.executionId == null) {
                long start = System.currentTimeMillis();
                AtomicLong counter = new AtomicLong(0);
                String executionId = execution.getId().toHexString();
                logger.warn("The root resolved plan nodes do not contains the execution id {}. Assuming the execution was created before Step 27.4, starting migration of its resolved plan nodes.", executionId);
                recursivelyAddExecutionIdToResolvedPlanNodes(executionId, r, counter);
                logger.warn("Migration for execution id {} completed in {}ms for {} resolved plan nodes.", executionId, System.currentTimeMillis() - start, counter.get());
            }
        });
    }

    private void recursivelyAddExecutionIdToResolvedPlanNodes(String executionId, ResolvedPlanNode resolvedPlanNode, AtomicLong counter) {
        ResolvedPlanNode updatedResolvedPlanNode = new ResolvedPlanNode(executionId, resolvedPlanNode.artefact,
                resolvedPlanNode.artefactHash, resolvedPlanNode.parentId, resolvedPlanNode.parentSource, resolvedPlanNode.position);
        updatedResolvedPlanNode.setId(resolvedPlanNode.getId());
        underlyingAccessor.save(updatedResolvedPlanNode);
        counter.incrementAndGet();
        underlyingAccessor.getByParentId(resolvedPlanNode.getId().toHexString()).forEach(r -> {
            recursivelyAddExecutionIdToResolvedPlanNodes(executionId, r, counter);
        });
    }

    private void store(ResolvedPlanNode resolvedPlanNode) {
        //We still get the root (parentID == null)) directly from the underlying accessor, we store only by non-null Parent ID
        if (resolvedPlanNode.parentId != null) {
            cacheByParentId.computeIfAbsent(resolvedPlanNode.parentId, resolvedPlanNodes -> new ArrayList<>()).add(resolvedPlanNode);
        }
    }

    public Stream<ResolvedPlanNode> getByParentId(String parentId) {
        //We need to sort by position, since this is called once by parent ID only, sorting here is fine
        List<ResolvedPlanNode> resolvedPlanNodes = cacheByParentId.get(parentId);
        return (resolvedPlanNodes != null) ? resolvedPlanNodes.stream().sorted(Comparator.comparingInt(r -> r.position)) :
                Stream.empty();
    }

    /**
     * This method can be used to directly get a node by ID from the underlying accessor directly
     */
    public ResolvedPlanNode getFromUnderlyingAccessor(String resolvedPlanNodeId) {
        return underlyingAccessor.get(resolvedPlanNodeId);
    }
}
