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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.yaml.YamlFields;
import step.core.yaml.deserializers.StepYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializerAddOn;

import java.io.IOException;
import java.util.Iterator;

@StepYamlDeserializerAddOn(targetClasses = {YamlDynamicInputs.class})
public class YamlDynamicInputDeserializer extends StepYamlDeserializer<YamlDynamicInputs> {

    private static final ObjectMapper jsonObjectMapper = DefaultJacksonMapperProvider.getObjectMapper();

    public YamlDynamicInputDeserializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }

    /**
     * @return dynamic input values as json string
     */
    private String deserializeDynamicInputs(ObjectCodec codec, ArrayNode value) throws JsonProcessingException {
        ObjectNode inputDynamicValues = (ObjectNode) codec.createObjectNode();
        Iterator<JsonNode> elements = value.elements();
        while (elements.hasNext()) {
            JsonNode next = elements.next();
            Iterator<String> fieldNames = next.fieldNames();
            while (fieldNames.hasNext()) {
                String inputName = fieldNames.next();
                JsonNode argumentValue = next.get(inputName);
                if (!argumentValue.isContainerNode()) {
                    // for simplified input values we also convert them to full dynamic values format (technical format)
                    // the technical format is used for persistence and UI
                    ObjectNode dynamicValue = (ObjectNode) codec.createObjectNode();
                    dynamicValue.put("dynamic", false);

                    if (argumentValue.isTextual()) {
                        dynamicValue.put(YamlFields.DYN_VALUE_VALUE_FIELD, argumentValue.asText());
                    } else if (argumentValue.isBoolean()) {
                        dynamicValue.put(YamlFields.DYN_VALUE_VALUE_FIELD, argumentValue.asBoolean());
                    } else if (argumentValue.isIntegralNumber()) {
                        dynamicValue.put(YamlFields.DYN_VALUE_VALUE_FIELD, argumentValue.asLong());
                    } else if (argumentValue.isFloatingPointNumber()) {
                        dynamicValue.put(YamlFields.DYN_VALUE_VALUE_FIELD, argumentValue.asDouble());
                    }
                    inputDynamicValues.set(inputName, dynamicValue);
                } else {
                    ObjectNode dynamicValue = (ObjectNode) codec.createObjectNode();
                    dynamicValue.put("dynamic", true);
                    JsonNode expression = argumentValue.get(YamlFields.DYN_VALUE_EXPRESSION_FIELD);
                    dynamicValue.put(YamlFields.DYN_VALUE_EXPRESSION_FIELD, expression == null ? "" : expression.asText());
                    inputDynamicValues.set(inputName, dynamicValue);
                }
            }

        }
        return jsonObjectMapper.writeValueAsString(inputDynamicValues);
    }

    @Override
    public YamlDynamicInputs deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        if (node instanceof ArrayNode) {
            return new YamlDynamicInputs(deserializeDynamicInputs(p.getCodec(), (ArrayNode) node));
        } else {
            return new YamlDynamicInputs(YamlDynamicInputSerializer.EMPTY_JSON);
        }
    }
}
