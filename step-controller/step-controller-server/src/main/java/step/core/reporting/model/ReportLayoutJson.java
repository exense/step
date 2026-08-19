package step.core.reporting.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.json.JsonObject;
import step.core.accessors.AbstractOrganizableObject;

public class ReportLayoutJson {

    public final String id;
    public final String name;
    public final ReportLayout.ReportLayoutType reportType;
    public final JsonObject layout;

    @JsonCreator
    public ReportLayoutJson(@JsonProperty("id") String id, @JsonProperty("name") String name,
                            @JsonProperty(value = "reportType", defaultValue = "SingleExecution") ReportLayout.ReportLayoutType reportType,
                            @JsonProperty("layout") JsonObject layout) {
        this.id = id;
        this.name = name;
        this.reportType = reportType != null ? reportType : ReportLayout.ReportLayoutType.SingleExecution;
        this.layout = layout;
    }

    public ReportLayoutJson(ReportLayout reportLayout) {
        this.id = reportLayout.getId().toString();
        this.name = reportLayout.getAttribute(AbstractOrganizableObject.NAME);
        this.reportType = reportLayout.reportType;
        this.layout = reportLayout.layout;
    }
}
