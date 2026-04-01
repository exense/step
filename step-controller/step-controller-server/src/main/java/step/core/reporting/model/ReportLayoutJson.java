package step.core.reporting.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.json.JsonObject;
import step.core.accessors.AbstractOrganizableObject;

public class ReportLayoutJson {

    public final String id;
    public final String name;
    public final JsonObject layout;

    @JsonCreator
    public ReportLayoutJson(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("layout") JsonObject layout) {
        this.id = id;
        this.name = name;
        this.layout = layout;
    }

    public ReportLayoutJson(ReportLayout reportLayout) {
        this.id = reportLayout.getId().toString();
        this.name = reportLayout.getAttribute(AbstractOrganizableObject.NAME);
        this.layout = reportLayout.layout;
    }
}
