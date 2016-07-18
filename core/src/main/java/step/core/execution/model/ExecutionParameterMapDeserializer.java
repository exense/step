package step.core.execution.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class ExecutionParameterMapDeserializer extends JsonDeserializer<Map<String, String>> {

	public ExecutionParameterMapDeserializer() {
	}

	@Override
	public Map<String, String> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		Map<String, String> result = new HashMap<String,String>();
		JsonNode node = jp.getCodec().readTree(jp);
		node.forEach(new Consumer<JsonNode>() {
			@Override
			public void accept(JsonNode t) {
				if(t.has("key")&&t.has("value")) {
					result.put(t.get("key").asText(), t.get("value").asText());					
				}
			}
		});
		return result;
	}
}
