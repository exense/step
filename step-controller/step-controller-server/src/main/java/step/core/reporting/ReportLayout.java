package step.core.reporting;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import step.core.accessors.AbstractTrackedObject;

public class ReportLayout extends AbstractTrackedObject {

    public static final String FIELD_IS_SHARED = "shared";

    @NotNull
    Map<String, Object> layout;
    boolean shared = false;

    public ReportLayout() {
    }

    public Map<String, Object> getLayout() {
        return layout;
    }

    public void setLayout(Map<String, Object> layout) {
        this.layout = layout;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }
}
