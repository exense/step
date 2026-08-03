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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Writes {@link EntityOrigin} as a bare string as long as it carries nothing but <code>by</code>, and as an object
 * as soon as any of the additional fields is set.
 */
public class EntityOriginSerializer extends JsonSerializer<EntityOrigin> {

    @Override
    public void serialize(EntityOrigin entityOrigin, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (entityOrigin.isEmpty()) {
            jsonGenerator.writeNull();
        } else if (entityOrigin.isPlainForm()) {
            jsonGenerator.writeString(entityOrigin.getBy());
        } else {
            jsonGenerator.writePOJO(new EntityOrigin.Fields(entityOrigin));
        }
    }
}
