package step.core.reporting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.json.JsonObject;
import step.core.accessors.AbstractTrackedObject;

public class ReportLayout extends AbstractTrackedObject {

    public enum ReportLayoutVisibility {
        Preset, Private, Shared
    }

    public static final String FIELD_VISIBILITY = "visibility";

    public JsonObject layout;
    public ReportLayoutVisibility visibility;

    @JsonCreator
    public ReportLayout(@JsonProperty("layout") JsonObject layout, @JsonProperty(value = "visibility", defaultValue = "Private") ReportLayoutVisibility visibility) {
        this.layout = layout;
        this.visibility = visibility != null ? visibility : ReportLayoutVisibility.Private;
    }
}
