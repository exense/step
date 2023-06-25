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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.artefacts.CallFunction;
import step.artefacts.FunctionGroup;
import step.core.dynamicbeans.DynamicValue;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.simple.YamlPlanFields;
import step.plans.simple.deserializers.SimpleArtefactFieldDeserializationProcessor;
import step.plans.simple.schema.SimplePlanJsonSchemaGenerator;
import step.plans.simple.serializers.SimpleArtefactFieldSerializationProcessor;

import static step.plans.simple.YamlPlanFields.*;
import static step.plans.simple.schema.SimplePlanJsonSchemaGenerator.CALL_KEYWORD_FUNCTION_NAME_DEF;

public class KeywordNameRule extends DynamicInputsSupport implements ArtefactFieldConversionRule {

    private static final String EMPTY_JSON = "{}";

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        // combines 'keyword' (keyword name) and 'selection' criteria
        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            if (objectClass.equals(CallFunction.class) && field.getName().equals(YamlPlanFields.CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD)) {
                JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();
                SimplePlanJsonSchemaGenerator.addRef(nestedPropertyParamsBuilder, CALL_KEYWORD_FUNCTION_NAME_DEF);
                propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
                return true;
            }
            return false;
        };
    }

    @Override
    public SimpleArtefactFieldDeserializationProcessor getArtefactFieldDeserializationProcessor() {
        return (artefactClass, field, output, codec) -> {
            // for 'CallFunction' we can use either the `keyword` (keyword name) field or the `keyword.selectionCriteria` to define the keyword name
            if (artefactClass.equals(CallFunction.ARTEFACT_NAME) && field.getKey().equals(CALL_FUNCTION_FUNCTION_SIMPLE_FIELD)) {
                JsonNode simpleFunctionValue = field.getValue();
                JsonNode functionSelectionCriteria = simpleFunctionValue.get(TOKEN_SELECTOR_TOKEN_SIMPLE_FIELD);

                // explicit function name as dynamic value
                if (functionSelectionCriteria != null) {
                    output.put(YamlPlanFields.TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD, deserializeDynamicInputs(codec, (ArrayNode) functionSelectionCriteria));
                } else {
                    output.set(YamlPlanFields.CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD, simpleFunctionValue);
                }
                return true;
            } else if ((artefactClass.equals(FunctionGroup.FUNCTION_GROUP_ARTEFACT_NAME) && field.getKey().equals(TOKEN_SELECTOR_TOKEN_SIMPLE_FIELD))) {
                // 'token' aka 'selectionCriteria' field  should contain all input values (dynamic values) as json string
                //  but in simplified format we represent input values as array of key / values
                String argumentsAsJsonString = deserializeDynamicInputs(codec, (ArrayNode) field.getValue());
                output.put(YamlPlanFields.TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD, argumentsAsJsonString);
                return true;
            } else {
                return false;
            }
        };
    }

    @Override
    public SimpleArtefactFieldSerializationProcessor getArtefactFieldSerializationProcessor() {
        // TODO: for function group also
        return (artefact, field, fieldMetadata, gen) -> {
            if (CallFunction.class.isAssignableFrom(artefact.getClass())) {
                if ((field.getName().equals(CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD))) {
                    DynamicValue<String> function = (DynamicValue<String>) field.get(artefact);
                    DynamicValue<String> token = ((CallFunction) artefact).getToken();

                    boolean useSelectionCriteria = (token.getValue() != null && !token.getValue().isEmpty() && !token.getValue().equals(EMPTY_JSON))
                            || (token.getExpression() != null && !token.getExpression().isEmpty());

                    gen.writeFieldName(CALL_FUNCTION_FUNCTION_SIMPLE_FIELD);
                    if (!useSelectionCriteria) {
                        gen.writeObject(function);
                    } else {
                        gen.writeStartObject();
                        gen.writeFieldName(TOKEN_SELECTOR_TOKEN_SIMPLE_FIELD);
                        gen.writeObject(token);
                        gen.writeEndObject();
                    }
                    return true;
                } else if (field.getName().equals(TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD)) {
                    // token is analyzed together with 'function' field
                    return true;
                }

            }
            return false;
        };
    }
}
