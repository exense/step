package step.core.artefacts.reports.resolvedplan;

import step.core.accessors.AbstractAccessor;
import step.core.collections.*;
import step.core.collections.filters.Equals;

import java.util.stream.Stream;

public class ResolvedPlanNodeAccessor extends AbstractAccessor<ResolvedPlanNode> {

    public static final String PARENT_ID = "parentId";
    public static final String EXECUTION_ID = "executionId";

    public ResolvedPlanNodeAccessor(CollectionFactory collectionFactory) {
        super(collectionFactory.getCollection("resolvedPlans", ResolvedPlanNode.class));
        createOrUpdateIndex(PARENT_ID);
        createOrUpdateIndex(EXECUTION_ID);
    }

    public Stream<ResolvedPlanNode> getByExecutionId(String executionId) {
        return this.collectionDriver.find(getFilterByExecutionId(executionId), null, (Integer)null, (Integer)null, 0);
    }

    private Filter getFilterByExecutionId(String executionId) {
        return Filters.equals(EXECUTION_ID, executionId);
    }

    public Stream<ResolvedPlanNode> getByParentId(String parentId) {
        Equals parentIdFilter = Filters.equals(PARENT_ID, parentId);
        return this.collectionDriver.find(parentIdFilter, new SearchOrder("position", 1), (Integer)null, (Integer)null, 0);
    }

    public void deleteByExecutionId(String executionId) {
        this.collectionDriver.remove(getFilterByExecutionId(executionId));
    }
}
