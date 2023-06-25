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
package step.plans.simple.rules;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.json.spi.JsonProvider;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.simple.serializers.SimpleArtefactFieldSerializationProcessor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class CommonFilteredFieldRule implements ArtefactFieldConversionRule {

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            // just skip these fields
            return isIgnoredField(field);
        };
    }

    @Override
    public SimpleArtefactFieldSerializationProcessor getArtefactFieldSerializationProcessor() {
        return (artefact, field, fieldMetadata, gen) -> isIgnoredField(field);
    }

    private boolean isIgnoredField(Field field) {
        return field.isSynthetic()
                || field.isAnnotationPresent(JsonIgnore.class)
                || field.getType().equals(Object.class)
                || Exception.class.isAssignableFrom(field.getType())
                || Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers());
    }
}
