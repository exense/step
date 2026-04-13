package step.core.yaml.deserialization;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserDelegate;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class PatchingParserDelegate extends JsonParserDelegate {

    private final Map<JsonToken, JsonLocation> locationForToken = new HashMap<>();

    private JsonLocation lastDistinctLocation;

    private final Deque<LocatedJsonNode> nodeStack = new ArrayDeque<>();

    protected final PatchingContext patchingContext;

    public PatchingParserDelegate(JsonParser d, PatchingContext context) {

        super(d);
        patchingContext = context;
    }

    @Override
    public JsonToken nextToken() throws IOException {
        JsonLocation preLocation = currentLocation();
        JsonToken token = super.nextToken();
        if (!preLocation.equals(currentLocation())) {
            lastDistinctLocation = preLocation;
        }
        locationForToken.put(token, preLocation);

        if (token == JsonToken.END_OBJECT && !nodeStack.isEmpty()) {
            LocatedJsonNode currentNode = nodeStack.pop();
            currentNode.setEndLocation(currentTokenLocation());
        }

        return token;
    }

    protected JsonLocation getLastLocationForToken(JsonToken token) {
        return locationForToken.get(token);
    }

    protected JsonLocation getLastDistinctLocation() {
        return lastDistinctLocation;
    }

    public void setCurrentObjectNode(LocatedJsonNode jsonNode) {
        jsonNode.setStartLocation(currentLocation());
        nodeStack.add(jsonNode);
    }

    protected PatchingContext getPatchingContext() {
        return this.patchingContext;
    }
}
