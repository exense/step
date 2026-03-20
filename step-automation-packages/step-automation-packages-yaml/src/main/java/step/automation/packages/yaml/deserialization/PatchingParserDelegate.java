package step.automation.packages.yaml.deserialization;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.automation.packages.yaml.LocatedJsonNode;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class PatchingParserDelegate extends JsonParserDelegate {

    private Map<JsonToken, JsonLocation> distinctLocationBeforeToken = new HashMap<>();

    private JsonLocation lastDistinctLocation;

    private final Deque<LocatedJsonNode> nodeStack = new ArrayDeque<>();

    public PatchingParserDelegate(JsonParser d) {
        super(d);
    }

    @Override
    public JsonToken nextToken() throws IOException {
        JsonLocation preLocation = currentLocation();
        JsonToken token = super.nextToken();
        if (!preLocation.equals(currentLocation())) {
            lastDistinctLocation = preLocation;
        }
        distinctLocationBeforeToken.put(token, lastDistinctLocation);

        if (token == JsonToken.END_OBJECT && !nodeStack.isEmpty()) {
            LocatedJsonNode currentNode = nodeStack.pop();
            currentNode.setEndLocation(currentLocation());
        }

        return token;
    }

    protected JsonLocation getDistinctLocationBeforeToken(JsonToken token) {
        return distinctLocationBeforeToken.get(token);
    }

    protected JsonLocation getLastDistinctLocation() {
        return lastDistinctLocation;
    }

    public ObjectNode setCurrentObjectNode(LocatedJsonNode jsonNode) {
        jsonNode.setStartLocation(currentLocation());
        nodeStack.add(jsonNode);
        return jsonNode;
    }
}
