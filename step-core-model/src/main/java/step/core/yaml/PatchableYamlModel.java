package step.core.yaml;

import com.fasterxml.jackson.core.JsonLocation;
import step.core.yaml.deserialization.PatchingContext;

public interface PatchableYamlModel {

    void setPatchingBounds(JsonLocation startLocation, JsonLocation endLocation);

    int getStartOffset();

    int getIndent();

    int getEndOffset();

    void setStartOffset(int startOffset);

    void setEndOffset(int endOffset);

    void setIndent(int indent);

    void setContext(PatchingContext context);
}
