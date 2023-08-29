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
package step.plans.parser.yaml.rules;

import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.core.dynamicbeans.DynamicValue;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.core.yaml.deserializers.YamlDynamicValueDeserializer;
import step.plans.parser.yaml.schema.YamlDynamicValueJsonSchemaHelper;
import step.core.yaml.serializers.YamlDynamicValueSerializer;

/**
 * @see YamlDynamicValueDeserializer
 * @see YamlDynamicValueSerializer
 */
public class DynamicFieldRule implements ArtefactFieldConversionRule {

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        YamlDynamicValueJsonSchemaHelper dynamicValueHelper = new YamlDynamicValueJsonSchemaHelper(jsonProvider);

        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            if (DynamicValue.class.isAssignableFrom(field.getType())) {
                JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();

                dynamicValueHelper.applyDynamicValueDefForField(field, nestedPropertyParamsBuilder);

                propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
                return true;
            }
            return false;
        };
    }

    // SERIALIZER/DESERIALIZER IS NOT REQUIRED, BECAUSE THIS LOGIC IS INCLUDED in YamlDynamicValueSerializer/YamlDynamicValueDeserializer
}
