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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.serializers.StepYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializerAddOn;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@StepYamlSerializerAddOn(targetClasses = {YamlKeywordDefinition.class})
public class YamlKeywordDefinitionSerializer extends StepYamlSerializer<YamlKeywordDefinition> {

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = DefaultJacksonMapperProvider.getObjectMapper();

    public YamlKeywordDefinitionSerializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }

    @Override
    public void serialize(YamlKeywordDefinition value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        DynamicValue<String> dynamicValue = value.toDynamicValue();
        String simpleFunctionName = getFunctionName(dynamicValue, true);
        if (simpleFunctionName != null) {
            gen.writeString(simpleFunctionName);
        } else {
            Map<String, DynamicValue<String>> selectionCriteria = getFunctionSelectionCriteriaForYamlSerialization(dynamicValue);
            if (selectionCriteria != null) {
                gen.writeStartObject();
                for (Map.Entry<String, DynamicValue<String>> entry : selectionCriteria.entrySet()) {
                    gen.writeObjectField(entry.getKey(), entry.getValue());
                }
                gen.writeEndObject();
            }
        }
    }

    public static String getFunctionName(DynamicValue<String> dynamicSelectionCriteria, boolean asSimpleFunctionName) throws JsonProcessingException {
        if (!dynamicSelectionCriteria.getValue().trim().isEmpty()) {
            String jsonValue = dynamicSelectionCriteria.getValue();
            return getFunctionName(jsonValue, asSimpleFunctionName);
        } else {
            return null;
        }
    }

    public static String getFunctionName(String jsonValue, boolean asSimpleFunctionName) throws JsonProcessingException {
        if (jsonValue == null || jsonValue.isEmpty()) {
            return null;
        }
        if (jsonValue.startsWith("{")) {
            TypeReference<HashMap<String, JsonNode>> functionValueTypeRef = new TypeReference<>() {
            };
            HashMap<String, JsonNode> functionNameAsMap = DEFAULT_OBJECT_MAPPER.readValue(jsonValue, functionValueTypeRef);

            JsonNode simpleFunctionName = null;
            if (!asSimpleFunctionName || functionNameAsMap.size() == 1) {
                simpleFunctionName = functionNameAsMap.get(AbstractOrganizableObject.NAME);
            }

            if (simpleFunctionName != null && !simpleFunctionName.isContainerNode()) {
                return simpleFunctionName.asText();
            } else {
                DynamicValue<String> dynamicValue = DEFAULT_OBJECT_MAPPER.treeToValue(simpleFunctionName, DynamicValue.class);
                if (dynamicValue != null && !dynamicValue.isDynamic()) {
                    return dynamicValue.getValue();
                } else {
                    return null;
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid function. Function selector for yaml only supports function selectors as jsons, but was: " + jsonValue);
        }
    }

    /**
     * Returns all selection criteria for keyword serialization
     */
    private Map<String, DynamicValue<String>> getFunctionSelectionCriteriaForYamlSerialization(DynamicValue<String> function) throws JsonProcessingException {
        if (!function.getValue().trim().isEmpty()) {
            if (function.getValue().startsWith("{")) {
                Map<String, DynamicValue<String>> result = new HashMap<>();
                TypeReference<HashMap<String, JsonNode>> functionValueTypeRef = new TypeReference<>() {
                };
                HashMap<String, JsonNode> selectionCriteriaMap = YamlKeywordDefinitionSerializer.DEFAULT_OBJECT_MAPPER.readValue(function.getValue(), functionValueTypeRef);
                for (Map.Entry<String, JsonNode> entry : selectionCriteriaMap.entrySet()) {
                    if (entry.getValue().isContainerNode()) {
                        result.put(entry.getKey(), YamlKeywordDefinitionSerializer.DEFAULT_OBJECT_MAPPER.treeToValue(entry.getValue(), DynamicValue.class));
                    } else {
                        result.put(entry.getKey(), new DynamicValue<>(entry.getValue().asText()));
                    }
                }
                return result;
            } else {
                throw new IllegalArgumentException("Invalid function. Function selector for yaml only supports function selectors as jsons, but was: " + function.getValue());
            }

        } else {
            return null;
        }
    }
}
