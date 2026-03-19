package step.automation.packages.yaml.deserialization;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserDelegate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PatchingParserDelegate extends JsonParserDelegate {

    private Map<JsonToken, JsonLocation> distinctLocationBeforeToken = new HashMap<>();

    private JsonLocation lastDistinctLocation;

    public PatchingParserDelegate(JsonParser d) {
        super(d);
    }

    @Override
    public JsonToken nextToken() throws IOException {
        JsonLocation preLocation = super.currentLocation();
        JsonToken token = super.nextToken();
        if (!preLocation.equals(super.currentLocation())) {
            lastDistinctLocation = preLocation;
        }
        distinctLocationBeforeToken.put(token, lastDistinctLocation);
        return token;
    }

    protected JsonLocation getDistinctLocationBeforeToken(JsonToken token) {
        return distinctLocationBeforeToken.get(token);
    }

    protected JsonLocation getLastDistinctLocation() {
        return lastDistinctLocation;
    }
}
