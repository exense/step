package step.core.accessors;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

// Used to deserialize Map<String, Object>. Per default jackson deserialize the map values as Map
public class MapSerializer extends JsonSerializer<Map<String, Object>> {

	private ObjectMapper mapper;
	
	public MapSerializer() {
		super();
		mapper = MapDeserializer.getMapper();
	}

	@Override
	public void serialize(Map<String, Object> value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		mapper.writeValue(gen, value);
	}
}
