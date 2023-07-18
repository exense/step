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

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;

public class EnumFieldRule implements ArtefactFieldConversionRule {
    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            if (field.getType().isEnum()) {
                JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();

                JsonArrayBuilder enumArray = jsonProvider.createArrayBuilder();
                for (Object enumValue : field.getType().getEnumConstants()) {
                    enumArray.add(enumValue.toString());
                }
                nestedPropertyParamsBuilder.add("enum", enumArray);

                propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
                return true;
            }
            return false;
        };
    }

}
