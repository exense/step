package step.core.yaml.deserialization;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class LocatedJsonObject extends ObjectNode implements LocatedJsonNode {

    private final PatchingContext patchingContext;
    private JsonLocation startLocation;
    private JsonLocation endLocation;

    public LocatedJsonObject(JsonNodeFactory nodeFactory, PatchingContext context) {
        super(nodeFactory);
        this.patchingContext = context;
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

    @Override
    public void setEndLocation(JsonLocation endLocation) {
        this.endLocation = endLocation;
    }

    @Override
    public void setStartLocation(JsonLocation startLocation) {
        this.startLocation = startLocation;
    }

    @Override
    public PatchingContext getPatchingContext() {
        return patchingContext;
    }
}
