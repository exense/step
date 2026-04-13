package step.core.yaml.deserialization;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LocatedYamlObjectFactory extends JsonNodeFactory {
    private final PatchingParserDelegate parser;

    public LocatedYamlObjectFactory(PatchingParserDelegate parser) {
        this.parser = parser;

    }

    @Override
    public ObjectNode objectNode() {
        LocatedJsonObject node = new LocatedJsonObject(this, parser.getPatchingContext());
        parser.setCurrentObjectNode(node);
        return node;
    }

    @Override
    public ArrayNode arrayNode() {
        LocatedJsonArray array = new LocatedJsonArray(this, parser.getPatchingContext());
        parser.setCurrentObjectNode(array);
        return array;
    }
}
