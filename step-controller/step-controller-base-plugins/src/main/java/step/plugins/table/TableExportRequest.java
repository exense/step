package step.plugins.table;

import step.framework.server.tables.service.TableRequest;

import java.util.List;

public class TableExportRequest {

    private TableRequest tableRequest;
    private List<String> fields;

    public TableRequest getTableRequest() {
        return tableRequest;
    }

    public void setTableRequest(TableRequest tableRequest) {
        this.tableRequest = tableRequest;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }
}
