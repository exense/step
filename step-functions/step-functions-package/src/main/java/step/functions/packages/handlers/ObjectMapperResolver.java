package step.functions.packages.handlers;


import java.io.IOException;

import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ObjectMapperResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public class ObjectIdSerializer extends JsonSerializer<ObjectId> {

        @Override
        public void serialize(ObjectId value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeString(value.toString());
        }
    }
    
    public ObjectMapperResolver() {
    	
    	SimpleModule module = new SimpleModule("ObjectIdmodule");
    	module.addSerializer(ObjectId.class, new ObjectIdSerializer());
        mapper = new ObjectMapper();
        mapper.registerModule(new JSONPModule());
        mapper.registerModule(module);
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}