package step.core.accessors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

// Used to deserialize Map<String, Object>. Per default jackson deserialize the map values as Map
public class MapDeserializer extends JsonDeserializer<Map<String, Object>> {

	private static Logger logger = LoggerFactory.getLogger(MapDeserializer.class);
	
	private ObjectMapper mapper;
	
	public MapDeserializer() {
		super();
		mapper = getMapper();
	}
	
	public static ObjectMapper getMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enableDefaultTyping();
        mapper.registerModule(new SimpleModule("jersey", new Version(1, 0, 0, null,null,null)) //
                .addSerializer(_id, _idSerializer()) //
                .addDeserializer(_id, _idDeserializer())); 
        return mapper;
	}

	private static Class<ObjectId> _id = ObjectId.class;

	private static JsonDeserializer<ObjectId> _idDeserializer() {
		return new JsonDeserializer<ObjectId>() {
			public ObjectId deserialize(JsonParser jp, DeserializationContext ctxt)
					throws IOException, JsonProcessingException {
				return new ObjectId(jp.readValueAs(String.class));
			}
		};
	}

	private static JsonSerializer<Object> _idSerializer() {
		return new JsonSerializer<Object>() {
			public void serialize(Object obj, JsonGenerator jsonGenerator, SerializerProvider provider)
					throws IOException, JsonProcessingException {
				jsonGenerator.writeString(obj == null ? null : obj.toString());
			}
		};
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
        		logger.warn("Error while deserializing key "+key, ex);
        	}
        });
		
		return result;
	}
}
