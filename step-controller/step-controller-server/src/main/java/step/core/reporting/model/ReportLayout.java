package step.core.reporting.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.json.JsonObject;
import step.core.accessors.AbstractTrackedObject;

public class ReportLayout extends AbstractTrackedObject {

    public enum ReportLayoutVisibility {
        Preset, Private, Shared
    }

    /**
     * Discriminates the report view a layout belongs to. Layouts are only ever listed within the
     * matching view. Absent value (legacy documents / legacy preset files) defaults to {@link ReportLayoutType#SingleExecution}.
     */
    public enum ReportLayoutType {
        SingleExecution, CrossExecution
    }

    public static final String FIELD_VISIBILITY = "visibility";
    public static final String FIELD_REPORT_TYPE = "reportType";

    public JsonObject layout;
    public ReportLayoutVisibility visibility;
    public ReportLayoutType reportType;

    @JsonCreator
    public ReportLayout(@JsonProperty("layout") JsonObject layout,
                        @JsonProperty(value = FIELD_VISIBILITY, defaultValue = "Private") ReportLayoutVisibility visibility,
                        @JsonProperty(value = FIELD_REPORT_TYPE, defaultValue = "SingleExecution") ReportLayoutType reportType) {
        this.layout = layout;
        this.visibility = visibility != null ? visibility : ReportLayoutVisibility.Private;
        this.reportType = reportType != null ? reportType : ReportLayoutType.SingleExecution;
    }

    /**
     * Convenience constructor defaulting the report type to {@link ReportLayoutType#SingleExecution}.
     */
    public ReportLayout(JsonObject layout, ReportLayoutVisibility visibility) {
        this(layout, visibility, ReportLayoutType.SingleExecution);
    }
}
