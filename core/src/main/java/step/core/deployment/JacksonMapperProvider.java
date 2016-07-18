package step.core.deployment;

import java.io.IOException;

import javax.ws.rs.ext.ContextResolver;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JacksonMapperProvider implements ContextResolver<ObjectMapper> {
	 
    private final ObjectMapper mapper;
 
    public JacksonMapperProvider() {
        mapper = createMapper();
    }
 
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
 
    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
//        mapper.configure(AUTO_DETECT_GETTERS, false);
//        mapper.configure(AUTO_DETECT_SETTERS, false);
//        mapper.setDeserializationConfig(mapper.getDeserializationConfig().without(FAIL_ON_UNKNOWN_PROPERTIES));
//        mapper.setSerializationConfig(mapper.getSerializationConfig().withSerializationInclusion(NON_DEFAULT));
//        mapper.setVisibilityChecker(Std.defaultInstance().withFieldVisibility(ANY));
// 
        mapper.registerModule(new SimpleModule("jersey", new Version(1, 0, 0, null,null,null)) //
                        .addSerializer(_id, _idSerializer()) //
                        .addDeserializer(_id, _idDeserializer()));
        return mapper;
    }
 
    private static Class<ObjectId> _id = ObjectId.class;
 
    private static JsonDeserializer<ObjectId> _idDeserializer() {
        return new JsonDeserializer<ObjectId>() {
            public ObjectId deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                return new ObjectId(jp.readValueAs(String.class));
            }
        };
    }
 
    private static JsonSerializer<Object> _idSerializer() {
        return new JsonSerializer<Object>() {
            public void serialize(Object obj, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException, JsonProcessingException {
                jsonGenerator.writeString(obj == null ? null : obj.toString());
            }
        };
    }
}
