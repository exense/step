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
package step.jsonschema;

import step.handlers.javahandler.jsonschema.FieldMetadata;
import step.handlers.javahandler.jsonschema.FieldMetadataExtractor;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;

import java.lang.reflect.Field;

public class DefaultFieldMetadataExtractor implements FieldMetadataExtractor {

    @Override
    public FieldMetadata extractMetadata(Class<?> objectClass, Field field) {
        JsonSchema schemaAnnotation = field.getAnnotation(JsonSchema.class);
        JsonSchemaFieldProcessor customProcessor = null;
        String defaultValue = null;
        boolean required = false;
        String fieldName = field.getName();
        if (schemaAnnotation != null) {
            customProcessor = getJsonSchemaFieldProcessorFromAnnotation(schemaAnnotation);

            if (schemaAnnotation.defaultProvider() != null && !schemaAnnotation.defaultProvider().equals(JsonSchemaDefaultValueProvider.None.class)) {
                try {
                    defaultValue = schemaAnnotation.defaultProvider().getDeclaredConstructor().newInstance().getDefaultValue(objectClass, field);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to prepare default value", e);
                }
            } else if (schemaAnnotation.defaultConstant() != null && !schemaAnnotation.defaultConstant().isEmpty()) {
                defaultValue = schemaAnnotation.defaultConstant();
            }
            required = schemaAnnotation.required();

            if (schemaAnnotation.fieldName() != null && !schemaAnnotation.fieldName().isEmpty()) {
                fieldName = schemaAnnotation.fieldName();
            }
        }

        if (customProcessor == null) {
            // lookup special json schema processor defined on class level
            JsonSchema classLevelAnnotation = field.getType().getAnnotation(JsonSchema.class);
            if (classLevelAnnotation != null) {
                customProcessor = getJsonSchemaFieldProcessorFromAnnotation(classLevelAnnotation);
            }
        }

        return new FieldMetadata(fieldName, defaultValue, field.getType(), field.getGenericType(), customProcessor, required);
    }

    protected JsonSchemaFieldProcessor getJsonSchemaFieldProcessorFromAnnotation(JsonSchema schemaAnnotation) {
        JsonSchemaFieldProcessor customProcessor = null;
        String ref = schemaAnnotation.ref() == null || schemaAnnotation.ref().isEmpty() ? null : schemaAnnotation.ref();
        if (ref != null) {
            customProcessor = new RefJsonSchemaFieldProcessor(ref);
        } else if (schemaAnnotation.customJsonSchemaProcessor() != null && !schemaAnnotation.customJsonSchemaProcessor().equals(JsonSchemaFieldProcessor.None.class)) {
            try {
                customProcessor = schemaAnnotation.customJsonSchemaProcessor().getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Unable to instantiate custom json schema processor");
            }
        }
        return customProcessor;
    }
}
