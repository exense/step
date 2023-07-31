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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.core.dynamicbeans.DynamicValue;
import step.plans.parser.yaml.YamlPlanFields;

import java.io.IOException;
import java.util.Iterator;

public class DynamicInputsSupport {

    private static final String EMPTY_JSON = "{}";

    protected final ObjectMapper jsonObjectMapper = new ObjectMapper();

    /**
     * @return dynamic input values as json string
     */
    protected String deserializeDynamicInputs(ObjectCodec codec, ArrayNode value) throws JsonProcessingException {
        ObjectNode inputDynamicValues = (ObjectNode) codec.createObjectNode();
        Iterator<JsonNode> elements = value.elements();
        while (elements.hasNext()) {
            JsonNode next = elements.next();
            Iterator<String> fieldNames = next.fieldNames();
            while (fieldNames.hasNext()) {
                String inputName = fieldNames.next();
                JsonNode argumentValue = next.get(inputName);
                if (!argumentValue.isContainerNode()) {
                    inputDynamicValues.set(inputName, argumentValue);
                } else {
                    ObjectNode dynamicValue = (ObjectNode) codec.createObjectNode();
                    dynamicValue.put("dynamic", true);
                    JsonNode expression = argumentValue.get(YamlPlanFields.DYN_VALUE_EXPRESSION_FIELD);
                    dynamicValue.put(YamlPlanFields.DYN_VALUE_EXPRESSION_FIELD, expression == null ? "" : expression.asText());
                    inputDynamicValues.set(inputName, dynamicValue);
                }
            }

        }
        return jsonObjectMapper.writeValueAsString(inputDynamicValues);
    }

    protected void serializeDynamicInputs(JsonGenerator gen, DynamicValue<String> dynamicInputsValue) throws IOException {
        gen.writeStartArray();
        if (dynamicInputsValue.isDynamic()) {
            throw new UnsupportedOperationException("Dynamic arguments are not supported");
        } else {
            String argumentValueString = dynamicInputsValue.getValue();
            if (argumentValueString != null && !argumentValueString.isEmpty()) {
                JsonNode argumentsJson = jsonObjectMapper.readTree(argumentValueString);
                Iterator<String> inputs = argumentsJson.fieldNames();
                while (inputs.hasNext()) {
                    String inputName = inputs.next();
                    gen.writeStartObject();
                    JsonNode dynamicInput = argumentsJson.get(inputName);
                    if (dynamicInput.isContainerNode() && dynamicInput.get("dynamic").asBoolean()) {
                        // dynamic input
                        gen.writeFieldName(inputName);
                        gen.writeStartObject();
                        gen.writeStringField(YamlPlanFields.DYN_VALUE_EXPRESSION_FIELD, dynamicInput.get(YamlPlanFields.DYN_VALUE_EXPRESSION_FIELD).asText());
                        gen.writeEndObject();
                    } else if (dynamicInput.isContainerNode()) {
                        // yaml input wrapped in dynamic value
                        gen.writeObjectField(inputName, dynamicInput.get("value"));
                    } else {
                        // primitive yaml input
                        gen.writeObjectField(inputName, dynamicInput);
                    }
                    gen.writeEndObject();
                }
            }
        }
        gen.writeEndArray();
    }

    protected boolean isEmptyDynamicInputs(DynamicValue<String> dynamicInputsValue) {
        return (dynamicInputsValue.getValue() != null && !dynamicInputsValue.getValue().isEmpty() && !dynamicInputsValue.getValue().equals(EMPTY_JSON))
                || (dynamicInputsValue.getExpression() != null && !dynamicInputsValue.getExpression().isEmpty());
    }
}
