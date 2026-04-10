package step.core.yaml.deserialization;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LocatedYamlObjectFactory extends JsonNodeFactory {
    private final PatchingParserDelegate parser;

    public LocatedYamlObjectFactory(PatchingParserDelegate parser) {
        this.parser = parser;

    }

    @Override
    public ObjectNode objectNode() {
        return parser.setCurrentObjectNode(new LocatedJsonNode(this, parser.getPatchingContext()));
    }
}
