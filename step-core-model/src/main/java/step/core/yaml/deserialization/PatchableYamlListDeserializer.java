/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
