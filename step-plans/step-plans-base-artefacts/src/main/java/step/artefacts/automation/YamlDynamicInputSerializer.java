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
package step.artefacts.automation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.YamlFields;
import step.core.yaml.serializers.StepYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializerAddOn;

import java.io.IOException;
import java.util.Iterator;

import static step.core.yaml.YamlFields.DYN_VALUE_DYNAMIC_FIELD;
import static step.core.yaml.YamlFields.DYN_VALUE_VALUE_FIELD;

@StepYamlSerializerAddOn(targetClasses = {YamlDynamicInputs.class})
public class YamlDynamicInputSerializer extends StepYamlSerializer<YamlDynamicInputs> {

    public static final String EMPTY_JSON = "{}";

    private static final ObjectMapper jsonObjectMapper = DefaultJacksonMapperProvider.getObjectMapper();

    public YamlDynamicInputSerializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }

    @Override
    public void serialize(YamlDynamicInputs value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        DynamicValue<String> dynamicInputsValue = value.toDynamicValue();
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
                    if (dynamicInput.isContainerNode() && dynamicInput.get(DYN_VALUE_DYNAMIC_FIELD).asBoolean()) {
                        // dynamic input
                        gen.writeFieldName(inputName);
                        gen.writeStartObject();
                        gen.writeStringField(YamlFields.DYN_VALUE_EXPRESSION_FIELD, dynamicInput.get(YamlFields.DYN_VALUE_EXPRESSION_FIELD).asText());
                        gen.writeEndObject();
                    } else if (dynamicInput.isContainerNode() && dynamicInput.has(DYN_VALUE_DYNAMIC_FIELD)) {
                        // yaml input wrapped in dynamic value
                        JsonNode dynamicInputValue = dynamicInput.get(DYN_VALUE_VALUE_FIELD);
                        //For arrays and objects the items or properties values (1st level)  can be dynamic too
                        if (dynamicInputValue.isObject()) {
                            gen.writeFieldName(inputName);
                            gen.writeStartObject();
                            //browse object properties and convert dynamic values if present
                            Iterator<String> nestedInputs = dynamicInputValue.fieldNames();
                            while (nestedInputs.hasNext()) {
                                String nestedInput = nestedInputs.next();
                                JsonNode nestedDynamicInput = dynamicInputValue.get(nestedInput);
                                if (nestedDynamicInput.get(DYN_VALUE_DYNAMIC_FIELD).asBoolean()) {
                                    gen.writeFieldName(nestedInput);
                                    gen.writeStartObject();
                                    gen.writeStringField(YamlFields.DYN_VALUE_EXPRESSION_FIELD, nestedDynamicInput.get(YamlFields.DYN_VALUE_EXPRESSION_FIELD).asText());
                                    gen.writeEndObject();
                                } else if (nestedDynamicInput.has(DYN_VALUE_DYNAMIC_FIELD)) {
                                    gen.writeObjectField(nestedInput, nestedDynamicInput.get(DYN_VALUE_VALUE_FIELD));
                                } else {
                                    gen.writeObjectField(nestedInput, nestedDynamicInput);
                                }
                            }
                            gen.writeEndObject();
                        } else if (dynamicInputValue.isArray()) {
                            gen.writeFieldName(inputName);
                            gen.writeStartArray();
                            ArrayNode arrayNode = (ArrayNode) dynamicInputValue;
                            for (int i=0; i < arrayNode.size(); i++) {
                                JsonNode itemNode = arrayNode.get(i);
                                if (itemNode.isContainerNode()) {
                                    if (itemNode.get(DYN_VALUE_DYNAMIC_FIELD).asBoolean()) {
                                        gen.writeStartObject();
                                        gen.writeStringField(YamlFields.DYN_VALUE_EXPRESSION_FIELD, itemNode.get(YamlFields.DYN_VALUE_EXPRESSION_FIELD).asText());
                                        gen.writeEndObject();
                                    } else if (itemNode.has(DYN_VALUE_DYNAMIC_FIELD)) {
                                        gen.writeObject(itemNode.get(DYN_VALUE_VALUE_FIELD));
                                    } else {
                                        gen.writeObject(itemNode);
                                    }
                                } else {
                                    gen.writeObject(itemNode);
                                }
                            }
                            gen.writeEndArray();
                        } else {
                            gen.writeObjectField(inputName, dynamicInputValue);
                        }
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

    @Override
    public boolean isEmpty(SerializerProvider provider, YamlDynamicInputs value) {
        DynamicValue<String> dynamicInputsValue = value.toDynamicValue();
        return !((dynamicInputsValue.getValue() != null && !dynamicInputsValue.getValue().isEmpty() && !dynamicInputsValue.getValue().equals(EMPTY_JSON))
                || (dynamicInputsValue.getExpression() != null && !dynamicInputsValue.getExpression().isEmpty()));
    }
}
