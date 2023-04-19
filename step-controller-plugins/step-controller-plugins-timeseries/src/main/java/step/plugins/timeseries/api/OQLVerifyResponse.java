package step.plugins.timeseries.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public class OQLVerifyResponse {

    @NotNull
    @JsonProperty("valid")
    private final boolean isValid;
    @NotNull
    @JsonProperty("hasUnknownFields")
    private final boolean hasUnknownFields;
    @NotNull
    private final Set<String> fields;

    public OQLVerifyResponse(boolean isValid, boolean hasUnknownFields, Set<String> fields) {
        this.isValid = isValid;
        this.hasUnknownFields = hasUnknownFields;
        this.fields = fields;
    }

    public boolean isValid() {
        return isValid;
    }

    public boolean hasUnknownFields() {
        return hasUnknownFields;
    }

    public Set<String> getFields() {
        return fields;
    }
}
