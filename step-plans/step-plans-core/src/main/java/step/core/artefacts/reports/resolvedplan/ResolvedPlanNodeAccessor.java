package step.core.artefacts.reports.resolvedplan;

import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;

import java.util.Map;
import java.util.stream.Stream;

public class ResolvedPlanNodeAccessor extends AbstractAccessor<ResolvedPlanNode> {

    public static final String PARENT_ID = "parentId";

    public ResolvedPlanNodeAccessor(CollectionFactory collectionFactory) {
        super(collectionFactory.getCollection("resolvedPlans", ResolvedPlanNode.class));
        createOrUpdateIndex(PARENT_ID);
    }

    public Stream<ResolvedPlanNode> getByParentId(String parentId) {
        return findManyByCriteria(Map.of(PARENT_ID, parentId));
    }
}
