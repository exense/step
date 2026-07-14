package step.core.reporting;

import step.core.accessors.AbstractAccessor;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.collections.SearchOrder;
import step.core.reporting.model.ReportLayout;

import java.util.List;
import java.util.stream.Collectors;

import static step.core.collections.Order.ASC;
import static step.core.reporting.model.ReportLayout.FIELD_REPORT_TYPE;
import static step.core.reporting.model.ReportLayout.FIELD_VISIBILITY;
import static step.core.reporting.model.ReportLayout.ReportLayoutType;
import static step.core.reporting.model.ReportLayout.ReportLayoutVisibility.Preset;
import static step.core.reporting.model.ReportLayout.ReportLayoutVisibility.Shared;

public class ReportLayoutAccessor extends AbstractAccessor<ReportLayout> {

    public ReportLayoutAccessor(Collection<ReportLayout> collectionDriver) {
        super(collectionDriver);
    }

    public List<ReportLayout> getAccessibleReportLayoutsDefinitions(String userName, ReportLayoutType reportType) {
        Filter ownerOrShared = Filters.or(List.of(Filters.equals(FIELD_VISIBILITY, Preset.name()), Filters.equals(FIELD_VISIBILITY, Shared.name()), Filters.equals("creationUser", userName)));
        Filter filter = Filters.and(List.of(Filters.equals(FIELD_REPORT_TYPE, reportType.name()), ownerOrShared));
        return this.getCollectionDriver()
            .find(filter, new SearchOrder(ATTRIBUTES_FIELD_NAME + "." + AbstractOrganizableObject.NAME, ASC.numeric), null, null, 0)
            .peek(reportLayout -> reportLayout.layout = null)
            .collect(Collectors.toList());
    }

}
