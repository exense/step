package step.core.yaml;

import com.fasterxml.jackson.core.JsonLocation;

public interface PatchableYamlModel {

    void setPatchingBounds(JsonLocation startLocation, int startFieldLocation, JsonLocation endLocation);

    int getStartOffset();

    int getIndent();

    int getEndOffset();

    void setPatchingBounds(PatchableYamlModel newBoundedArtefact);

    int getStartFieldOffset();
}
