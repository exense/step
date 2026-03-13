package step.automation.packages.yaml.deserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.core.yaml.SerializationUtils;

import java.io.IOException;
import java.util.*;

public class AdditionalFieldHandler extends DeserializationProblemHandler  {

    private final ObjectMapper objectMapper;
    private final Map<String, List<?>> additionalFields = new HashMap<>();
    private final AutomationPackageSerializationRegistry registry;
    
    public AdditionalFieldHandler(AutomationPackageSerializationRegistry registry, ObjectMapper mapper) {
        this.registry = registry;
        objectMapper = mapper;
    }
    
    @Override
    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException {
        
        JsonNode node = p.getCodec().readTree(p);

        // acquire reader for the right type
        Class<?> targetClass = registry.resolveClassForYamlField(propertyName);
        if (targetClass == null) return false;
            
        List<?> list = objectMapper.readerForListOf(targetClass).readValue(node);
        additionalFields.put(propertyName,  list);
                

        return false;
    }
    
    public Map<String, List<?>> getAdditionalFields() {
        return additionalFields;
    }
}
