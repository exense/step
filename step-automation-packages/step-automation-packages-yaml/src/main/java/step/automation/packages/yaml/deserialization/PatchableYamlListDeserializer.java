package step.automation.packages.yaml.deserialization;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.std.CollectionDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import step.core.yaml.PatchableYamlList;

import java.io.IOException;
import java.util.Collection;

public class PatchableYamlListDeserializer extends CollectionDeserializer {

    public PatchableYamlListDeserializer(CollectionDeserializer delegate) {
        super(delegate);
    }

    @Override
    public Collection<Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p instanceof PatchingParserDelegate) {
            PatchingParserDelegate patchingParser = (PatchingParserDelegate) p;
            JsonLocation startFieldLocation = patchingParser.getDistinctLocationBeforeToken(JsonToken.FIELD_NAME);
            JsonLocation startLocation = patchingParser.getDistinctLocationBeforeToken(JsonToken.START_ARRAY);
            Collection<Object> entity = super.deserialize(p, ctxt);
            PatchableYamlList<Object> patchableYamlList = new PatchableYamlList<>(entity);
            patchableYamlList.setPatchingBounds(startLocation, (int) startFieldLocation.getCharOffset(), patchingParser.currentLocation());
            return patchableYamlList;
        }
        return super.deserialize(p, ctxt);
    }


    @Override
    protected CollectionDeserializer withResolved(
        JsonDeserializer<?> keyDeser,
        JsonDeserializer<?> valueDeser,
        TypeDeserializer valueTypeDeser,
        NullValueProvider nuller,
        Boolean unwrapSingle) {
        CollectionDeserializer resolved = super.withResolved(keyDeser, valueDeser, valueTypeDeser, nuller, unwrapSingle);
        // Return your custom instance instead of the default one
        return new PatchableYamlListDeserializer(resolved);
    }
}
