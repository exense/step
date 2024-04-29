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
package step.core.yaml.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * The serialization template for generic classes having the following representation in yaml.
 * <pre>{@code
 * className:
 *    fieldA: valueA
 *    fieldA: valueB
 *    ...
 * }</pre>
 * The `className` is used to resolve the target java class, and all nested fields (`fieldA`, `fieldB`) are the fields
 * of this class.
 */
public abstract class NamedEntityYamlSerializer<T> {

    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String name = resolveYamlName(value);
        gen.writeStartObject();
        gen.writeFieldName(name);
        writeObject(value, gen);
        gen.writeEndObject();
    }

    protected void writeObject(T value, JsonGenerator gen) throws IOException {
        gen.writeObject(value);
    }

    protected abstract String resolveYamlName(T value);
}
