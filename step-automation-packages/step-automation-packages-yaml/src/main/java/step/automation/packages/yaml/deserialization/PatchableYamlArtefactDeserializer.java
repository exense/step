package step.automation.packages.yaml.deserialization;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import step.plans.parser.yaml.PatchableYamlArtefact;

import java.io.IOException;

public class PatchableYamlArtefactDeserializer<T extends PatchableYamlArtefact> extends JsonDeserializer<T> implements ContextualDeserializer {

    private final JsonDeserializer<T> delegate;

    public PatchableYamlArtefactDeserializer(JsonDeserializer<?> delegate) {
        this.delegate = (JsonDeserializer<T>) delegate;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p instanceof PatchingParserDelegate) {
            PatchingParserDelegate patchingParser = (PatchingParserDelegate) p;
            if (p.getLastClearedToken() == JsonToken.END_OBJECT) {
                p.getLastClearedToken();
            }
            JsonLocation startList = patchingParser.getDistinctLocationBeforeToken(JsonToken.FIELD_NAME);
            int startListItemOffset = 0;
            if (p.getLastClearedToken() == JsonToken.END_OBJECT) {
                startListItemOffset = (int) patchingParser.getDistinctLocationBeforeToken(JsonToken.END_OBJECT).getCharOffset();
            } else {
                startListItemOffset = (int) patchingParser.getDistinctLocationBeforeToken(JsonToken.START_ARRAY).getCharOffset() + 1;
            }
            JsonLocation startItem = patchingParser.currentLocation();
            T entity = delegate.deserialize(p, ctxt);
            entity.setPatchingBounds(startItem, startList, startListItemOffset, patchingParser.getLastDistinctLocation());

            return entity;
        }
        return delegate.deserialize(p, ctxt);
    }
/*
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt, T intoValue) throws IOException {
        if (p instanceof PatchingParserDelegate) {
            PatchingParserDelegate patchingParser = (PatchingParserDelegate) p;
            JsonLocation before = p.currentLocation();
            T entity = delegate.deserialize(p, ctxt, intoValue);
            entity.setPatchingBounds(before, p.currentLocation());

            return entity;
        }
        return delegate.deserialize(p, ctxt, intoValue);
    }*/

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
                                                BeanProperty property) throws JsonMappingException {
        JsonDeserializer<?> contextual = delegate;
        if (delegate instanceof ContextualDeserializer) {
            // make sure to propagate createContextual to the delegate
            contextual = ((ContextualDeserializer) contextual).createContextual(ctxt, property);
        }
        return new PatchableYamlArtefactDeserializer<>((JsonDeserializer<T>) contextual);
    }

}
