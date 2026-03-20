package step.automation.packages.yaml.deserialization;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import step.core.yaml.PatchableYamlModel;

import java.io.IOException;

public class PatchableYamlModelDeserializer<T extends PatchableYamlModel> extends JsonDeserializer<T> implements ContextualDeserializer {

    private final JsonDeserializer<T> delegate;

    public PatchableYamlModelDeserializer(JsonDeserializer<?> delegate) {
        this.delegate = (JsonDeserializer<T>) delegate;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p instanceof PatchingParserDelegate) {
            PatchingParserDelegate patchingParser = (PatchingParserDelegate) p;
            JsonLocation startList = patchingParser.getDistinctLocationBeforeToken(JsonToken.FIELD_NAME);
            int startListItemOffset = 0;
            if (p.getLastClearedToken() == JsonToken.END_OBJECT) {
                startListItemOffset = (int) patchingParser.getDistinctLocationBeforeToken(JsonToken.END_OBJECT).getCharOffset();
            } else {
                startListItemOffset = (int) patchingParser.getDistinctLocationBeforeToken(JsonToken.START_ARRAY).getCharOffset() + 1;
            }
            JsonLocation startItem = patchingParser.currentLocation();
            T entity = delegate.deserialize(p, ctxt);
            entity.setPatchingBounds(startItem, startListItemOffset, patchingParser.getLastDistinctLocation());

            return entity;
        }
        return delegate.deserialize(p, ctxt);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
                                                BeanProperty property) throws JsonMappingException {
        JsonDeserializer<?> contextual = delegate;
        if (contextual instanceof ContextualDeserializer) {
            // make sure to propagate createContextual to the delegate
            contextual = ((ContextualDeserializer) contextual).createContextual(ctxt, property);
        }
        if (contextual instanceof ResolvableDeserializer) {
            ((ResolvableDeserializer) contextual).resolve(ctxt);
        }
        return new PatchableYamlModelDeserializer<>((JsonDeserializer<T>) contextual);
    }
}
