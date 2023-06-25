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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.artefacts.CallFunction;
import step.core.dynamicbeans.DynamicValue;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.simple.YamlPlanFields;
import step.plans.simple.deserializers.SimpleArtefactFieldDeserializationProcessor;
import step.plans.simple.schema.SimpleDynamicValueJsonSchemaHelper;
import step.plans.simple.schema.SimplePlanJsonSchemaGenerator;
import step.plans.simple.serializers.SimpleArtefactFieldSerializationProcessor;

public class KeywordInputsRule extends DynamicInputsSupport implements ArtefactFieldConversionRule {

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            if (objectClass.equals(CallFunction.class) && field.getName().equals(YamlPlanFields.CALL_FUNCTION_ARGUMENT_ORIGINAL_FIELD)) {
                JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();
                SimplePlanJsonSchemaGenerator.addRef(nestedPropertyParamsBuilder, SimpleDynamicValueJsonSchemaHelper.DYNAMIC_KEYWORD_INPUTS_DEF);
                propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
                return true;
            }
            return false;
        };
    }

    @Override
    public SimpleArtefactFieldDeserializationProcessor getArtefactFieldDeserializationProcessor() {
        // 'argument' field for 'CallKeyword' artifact should contain all input values (dynamic values) as json string
        // but in simplified format we represent input values as array of key / values
        return (artefactClass, field, output, codec) -> {
            try {
                boolean inputsForCallFunction = artefactClass.equals(CallFunction.ARTEFACT_NAME)
                        && field.getKey().equals(YamlPlanFields.CALL_FUNCTION_ARGUMENT_SIMPLE_FIELD);

                if (inputsForCallFunction) {
                    String argumentsAsJsonString = deserializeDynamicInputs(codec, (ArrayNode) field.getValue());
                    output.put(YamlPlanFields.CALL_FUNCTION_ARGUMENT_ORIGINAL_FIELD, argumentsAsJsonString);
                    return true;
                } else {
                    return false;
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Unable to deserialize the 'argument' field", e);
            }
        };
    }

    @Override
    public SimpleArtefactFieldSerializationProcessor getArtefactFieldSerializationProcessor() {
        return (artefact, field, fieldMetadata, gen) -> {
            if (artefact.getClass().equals(CallFunction.class) && field.getName().equals(YamlPlanFields.CALL_FUNCTION_ARGUMENT_ORIGINAL_FIELD)) {
                DynamicValue<String> argumentValue = (DynamicValue<String>) field.get(artefact);
                gen.writeFieldName(YamlPlanFields.CALL_FUNCTION_ARGUMENT_SIMPLE_FIELD);
                serializeDynamicInputs(gen, argumentValue);
                return true;
            }
            return false;
        };
    }


}
