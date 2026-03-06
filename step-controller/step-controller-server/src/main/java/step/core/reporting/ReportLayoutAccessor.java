package step.core.reporting;

import step.core.accessors.AbstractAccessor;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.collections.SearchOrder;
import step.core.collections.filters.Or;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static step.core.collections.Order.ASC;
import static step.core.reporting.ReportLayout.FIELD_IS_SHARED;

public class ReportLayoutAccessor extends AbstractAccessor<ReportLayout> {

    public ReportLayoutAccessor(Collection<ReportLayout> collectionDriver) {
        super(collectionDriver);
    }

    public List<ReportLayout> getAccessibleReportLayoutsDefinitions(String username) {
        Or ownerOrShared = Filters.or(List.of(Filters.equals(FIELD_IS_SHARED, true), Filters.equals("creationUser", username)));
        return this.getCollectionDriver()
                .find(ownerOrShared, new SearchOrder(ATTRIBUTES_FIELD_NAME + "." + AbstractOrganizableObject.NAME, ASC.numeric), null, null, 0)
                .peek(reportLayout -> reportLayout.setLayout(Map.of()))
                .collect(Collectors.toList());
    }
}
