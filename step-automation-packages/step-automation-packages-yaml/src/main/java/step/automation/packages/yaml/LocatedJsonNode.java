package step.automation.packages.yaml;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

import javax.json.Json;

public class LocatedJsonNode extends ObjectNode {

    private JsonLocation startLocation;
    private JsonLocation endLocation;

    public LocatedJsonNode(JsonNodeFactory nodeFactory) {
        super(nodeFactory);
    }

    @Override
    public JsonParser traverse(ObjectCodec codec) {
        // wrap the default TreeTraversingParser with your own delegate
        return new LocationAwareTreeTraversingParser(super.traverse(codec), this);
    }

    @Override
    public JsonParser traverse() {
        return new LocationAwareTreeTraversingParser(super.traverse(), this);
    }

    public JsonLocation getStartLocation() {
        return startLocation;
    }

    public void setEndLocation(JsonLocation endLocation) {
        this.endLocation = endLocation;
    }

    public void setStartLocation(JsonLocation startLocation) {
        this.startLocation = startLocation;
    }
}
