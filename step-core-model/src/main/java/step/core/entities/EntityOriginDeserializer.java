/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
package step.core.entities;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Reads both the plain form (<code>origin: ai</code>) and the object form
 * (<code>origin: {by: ai, model: ...}</code>) of {@link EntityOrigin}.
 */
public class EntityOriginDeserializer extends JsonDeserializer<EntityOrigin> {

    @Override
    public EntityOrigin deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonToken currentToken = jsonParser.currentToken();
        if (currentToken == JsonToken.VALUE_NULL) {
            return null;
        } else if (currentToken == JsonToken.VALUE_STRING) {
            return new EntityOrigin(jsonParser.getValueAsString());
        } else if (currentToken == JsonToken.START_OBJECT) {
            return jsonParser.readValueAs(EntityOrigin.Fields.class).toEntityOrigin();
        } else {
            return (EntityOrigin) deserializationContext.handleUnexpectedToken(EntityOrigin.class, jsonParser);
        }
    }
}
