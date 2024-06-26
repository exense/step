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

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import step.handlers.javahandler.jsonschema.FieldMetadata;
import step.handlers.javahandler.jsonschema.JsonSchemaCreator;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;

import java.lang.reflect.Field;
import java.util.List;

public class EnumFieldProcessor implements JsonSchemaFieldProcessor {

    public EnumFieldProcessor() {
    }

    @Override
    public boolean applyCustomProcessing(Class<?> aClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder jsonObjectBuilder, List<String> list, JsonSchemaCreator schemaCreator) throws JsonSchemaPreparationException {
        if (field.getType().isEnum()) {
            JsonObjectBuilder nestedPropertyParamsBuilder = schemaCreator.getJsonProvider().createObjectBuilder();

            JsonArrayBuilder enumArray = schemaCreator.getJsonProvider().createArrayBuilder();
            for (Object enumValue : field.getType().getEnumConstants()) {
                enumArray.add(enumValue.toString());
            }
            nestedPropertyParamsBuilder.add("enum", enumArray);
            jsonObjectBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
            return true;
        }
        return false;
    }
}
