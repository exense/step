package step.controller.services.bulk;

import step.framework.server.tables.service.TableFilter;

import java.util.List;

public class BulkOperationParameters {

    private boolean preview = true;

    private BulkOperationTargetType targetType;

    private List<String> ids;
    private List<TableFilter> filters;

    public boolean isPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    public BulkOperationTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(BulkOperationTargetType targetType) {
        this.targetType = targetType;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    public List<TableFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<TableFilter> filters) {
        this.filters = filters;
    }
}
