package step.functions.handler.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;

public class JsonObjectDeserializer extends JsonDeserializer<JsonObject> {

    private static ObjectMapper objectMapper;

    @Override
    public JsonObject deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        // Read JSON as a string
        String json = jsonParser.getCodec().readTree(jsonParser).toString();
        // Use javax.json to parse the string to JsonObject
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }

    public static synchronized ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            // Register the custom deserializer for JsonObject
            SimpleModule module = new SimpleModule();
            module.addDeserializer(JsonObject.class, new JsonObjectDeserializer());
            objectMapper.registerModule(module);
        }
        return objectMapper;
    }
}
