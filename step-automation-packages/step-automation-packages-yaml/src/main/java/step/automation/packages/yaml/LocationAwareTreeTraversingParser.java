package step.automation.packages.yaml;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import step.automation.packages.yaml.deserialization.PatchingParserDelegate;


public class LocationAwareTreeTraversingParser extends PatchingParserDelegate {

    private final LocatedJsonNode sourceNode;

    public LocationAwareTreeTraversingParser(JsonParser delegate,
                                             LocatedJsonNode sourceNode) {
        super(delegate);
        this.sourceNode = sourceNode;
    }

    @Override
    public JsonLocation currentLocation() {
        // return the captured location instead of the default one
        // which would just point to the original source
        return sourceNode.getStartLocation();
    }

    public LocatedJsonNode getSourceNode() {
        return sourceNode;
    }
}
