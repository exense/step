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
            JsonLocation startItem = patchingParser.currentLocation();
            T entity = delegate.deserialize(p, ctxt);
            entity.setPatchingBounds(startItem, patchingParser.getLastDistinctLocation());
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
        return new PatchableYamlModelDeserializer<>(contextual);
    }
}
