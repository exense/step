package step.automation.packages.yaml;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.automation.packages.yaml.deserialization.PatchingParserDelegate;

public class LocatedYamlObjectFactory extends JsonNodeFactory {
    private final PatchingParserDelegate parser;

    public LocatedYamlObjectFactory(PatchingParserDelegate parser) {
        this.parser = parser;

    }

    @Override
    public ObjectNode objectNode() {
        return parser.setCurrentObjectNode(new LocatedJsonNode(this));
    }
}
