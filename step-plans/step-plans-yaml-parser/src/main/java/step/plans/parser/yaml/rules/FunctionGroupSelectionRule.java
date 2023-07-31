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

import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.artefacts.FunctionGroup;
import step.core.dynamicbeans.DynamicValue;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.deserializers.YamlArtefactFieldDeserializationProcessor;
import step.plans.parser.yaml.schema.YamlDynamicValueJsonSchemaHelper;
import step.plans.parser.yaml.schema.YamlPlanJsonSchemaGenerator;
import step.plans.parser.yaml.serializers.YamlArtefactFieldSerializationProcessor;

public class FunctionGroupSelectionRule extends DynamicInputsSupport implements ArtefactFieldConversionRule {

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
       return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            if (FunctionGroup.class.isAssignableFrom(objectClass) && field.getName().equals(YamlPlanFields.TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD)) {
                JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();
                YamlPlanJsonSchemaGenerator.addRef(nestedPropertyParamsBuilder, YamlDynamicValueJsonSchemaHelper.DYNAMIC_KEYWORD_INPUTS_DEF);
                propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
                return true;
            }
            return false;
        };
    }

    @Override
    public YamlArtefactFieldDeserializationProcessor getArtefactFieldDeserializationProcessor() {
        return (artefactClass, field, output, codec) -> {
            if ((artefactClass.equals(FunctionGroup.FUNCTION_GROUP_ARTEFACT_NAME) && field.getKey().equals(YamlPlanFields.TOKEN_SELECTOR_TOKEN_YAML_FIELD))) {
                // 'token' aka 'selectionCriteria' field  should contain all input values (dynamic values) as json string
                //  but in simplified format we represent input values as array of key / values
                String argumentsAsJsonString = deserializeDynamicInputs(codec, (ArrayNode) field.getValue());
                output.put(YamlPlanFields.TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD, argumentsAsJsonString);
                return true;
            }
            return false;
        };
    }

    @Override
    public YamlArtefactFieldSerializationProcessor getArtefactFieldSerializationProcessor() {
        return (artefact, field, fieldMetadata, gen) -> {
            if (FunctionGroup.class.isAssignableFrom(artefact.getClass()) && field.getName().equals(YamlPlanFields.TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD)) {
                // convert token to 'selectionCriteria' format
                DynamicValue<String> token = (DynamicValue<String>) field.get(artefact);
                if (!isEmptyDynamicInputs(token)) {
                    gen.writeFieldName(YamlPlanFields.TOKEN_SELECTOR_TOKEN_YAML_FIELD);
                    serializeDynamicInputs(gen, token);
                }
                return true;
            }
            return false;
        };
    }
}
