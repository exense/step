package step.grid.io;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr353.JSR353Module;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class ObjectMapperResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public ObjectMapperResolver() {
        System.out.println("new ObjectMapperResolver()");
        mapper = new ObjectMapper();
        mapper.registerModule(new JSR353Module());
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        System.out.println("ObjectMapperResolver.getContext(...)");
        return mapper;
    }
}