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
package step.core.yaml.schema;

import jakarta.json.JsonObjectBuilder;
import step.handlers.javahandler.jsonschema.*;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import static step.handlers.javahandler.JsonObjectMapper.getTypeClass;
import static step.handlers.javahandler.JsonObjectMapper.resolveGenericTypeForArrayOrCollection;

/**
 * Provides the following json schema for arrays or collections:
 * {
 *   "type" : "array",
 *   "items" : {
 *     "type" : "object",
 *     "properties" : {
 *       "someField": "all fields resolved via reflection"
 *     },
 *     "additionalProperties": false
 *   }
 * }
 */
public class CollectionFieldProcessor implements JsonSchemaFieldProcessor {

    @Override
    public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput, JsonSchemaCreator schemaCreator) throws JsonSchemaPreparationException {
        if (field.getType().isArray() || Collection.class.isAssignableFrom(field.getType())) {

            Class<?> elementType = null;
            try {
                elementType = getTypeClass(resolveGenericTypeForArrayOrCollection(fieldMetadata.getGenericType()));
            } catch (Exception ex) {
                // unresolvable generic type
            }

            if (elementType != null) {
                String itemType = JsonInputConverter.resolveJsonPropertyType(elementType);
                if ("object".equals(itemType)) {
                    YamlJsonSchemaHelper schemaHelper = new YamlJsonSchemaHelper(schemaCreator.getJsonProvider());
                    propertiesBuilder.add("type", "array");
                    propertiesBuilder.add("items", schemaHelper.createJsonSchemaForClass(schemaCreator, elementType, false));
                    return true;
                }
            }
        }
        return false;

    }
}
