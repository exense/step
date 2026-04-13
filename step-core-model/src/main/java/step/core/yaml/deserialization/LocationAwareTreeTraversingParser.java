package step.core.yaml.deserialization;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;


public class LocationAwareTreeTraversingParser extends PatchingParserDelegate {

    private final LocatedJsonNode sourceNode;

    public LocationAwareTreeTraversingParser(JsonParser delegate,
                                             LocatedJsonNode sourceNode) {
        super(delegate, sourceNode.getPatchingContext());
        this.sourceNode = sourceNode;
    }

    @Override
    public JsonLocation currentLocation() {
        return sourceNode.getStartLocation();
    }
}
