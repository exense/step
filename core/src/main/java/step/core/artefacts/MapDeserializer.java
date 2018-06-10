package step.core.artefacts;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

// Used to deserialize Map<String, Object>. Per default jackson deserialize the map values as Map
public class MapDeserializer extends JsonDeserializer<Map<String, Object>> {

	private ObjectMapper mapper;
	
	public MapDeserializer() {
		super();
		mapper = new ObjectMapper();
		mapper.enableDefaultTyping();
	}

	@Override
	public Map<String, Object> deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonNode node = jp.readValueAsTree();
		
		Map<String, Object> result = new HashMap<>();
		ObjectNode o = (ObjectNode) node;
        o.fields().forEachRemaining(e -> {
        	String key = e.getKey();
        	JsonNode eNode = e.getValue();
        	try {
        		result.put(key, mapper.treeToValue(eNode, Object.class));
        	} catch (Throwable ex) {
        		// Ignore these exception as it can be a ClassNotFoundException
        	}
        });
		
		return result;
	}
}
