package step.core.artefacts;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

public class MapSerializer extends JsonSerializer<Map<String, Object>> {

	private ObjectMapper mapper;
	
	public MapSerializer() {
		super();
		// TODO Auto-generated constructor stub
		mapper = new ObjectMapper();
		mapper.enableDefaultTyping();
	}

	@Override
	public void serialize(Map<String, Object> value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		gen.writeStartObject();
		for(String key:value.keySet()) {
	        gen.writeFieldName(key);
	        mapper.writeValue(gen, value);
		}
		gen.writeEndObject();
	}
}
