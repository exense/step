package step.core.yaml.deserialization;

import com.fasterxml.jackson.core.JsonLocation;

public interface LocatedJsonNode {
    void setEndLocation(JsonLocation endLocation);
    void setStartLocation(JsonLocation endLocation);

    JsonLocation getStartLocation();

    PatchingContext getPatchingContext();
}
