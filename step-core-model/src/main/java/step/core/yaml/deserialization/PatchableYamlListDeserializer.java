package step.core.yaml.deserialization;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.std.CollectionDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class PatchableYamlListDeserializer extends CollectionDeserializer {


    private final CollectionDeserializer delegate;

    public PatchableYamlListDeserializer(CollectionDeserializer delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    @Override
    public Collection<Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p instanceof PatchingParserDelegate) {
            PatchingParserDelegate patchingParser = (PatchingParserDelegate) p;
            JsonLocation startLocation = patchingParser.getLastLocationForToken(JsonToken.FIELD_NAME);
            Collection<Object> entity = delegate.deserialize(p, ctxt, new ArrayList<>());
            PatchableYamlList<Object> patchableYamlList = new PatchableYamlList<>(entity, patchingParser.getPatchingContext(), patchingParser.currentName());
            patchableYamlList.setPatchingBounds(startLocation, patchingParser.getLastDistinctLocation());
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
        return new PatchableYamlListDeserializer(resolved);
    }
}
