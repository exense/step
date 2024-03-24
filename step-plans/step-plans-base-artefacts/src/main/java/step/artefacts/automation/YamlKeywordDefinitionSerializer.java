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

    private final ObjectMapper simpleObjectMapper = DefaultJacksonMapperProvider.getObjectMapper();

    public YamlKeywordDefinitionSerializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }

    @Override
    public void serialize(YamlKeywordDefinition value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        DynamicValue<String> dynamicValue = value.toDynamicValue();
        String simpleFunctionName = getSimpleFunctionName(dynamicValue, simpleObjectMapper);
        if (simpleFunctionName != null) {
            gen.writeString(simpleFunctionName);
        } else {
            Map<String, DynamicValue<String>> selectionCriteria = getFunctionSelectionCriteriaForYamlSerialization(dynamicValue, simpleObjectMapper);
            if (selectionCriteria != null) {
                gen.writeStartObject();
                for (Map.Entry<String, DynamicValue<String>> entry : selectionCriteria.entrySet()) {
                    gen.writeObjectField(entry.getKey(), entry.getValue());
                }
                gen.writeEndObject();
            }
        }
    }

    /**
     * Returns the simple function name for yaml format (if the function contains only the 'name' criteria).
     * Returns null otherwise.
     */
    public static String getSimpleFunctionName(DynamicValue<String> dynamicSelectionCriteria, ObjectMapper jsonObjectMapper) throws JsonProcessingException {
        if (!dynamicSelectionCriteria.getValue().trim().isEmpty()) {
            if (dynamicSelectionCriteria.getValue().startsWith("{")) {
                TypeReference<HashMap<String, JsonNode>> functionValueTypeRef = new TypeReference<>() {
                };
                HashMap<String, JsonNode> functionNameAsMap = jsonObjectMapper.readValue(dynamicSelectionCriteria.getValue(), functionValueTypeRef);

                JsonNode simpleFunctionName = null;
                if (functionNameAsMap.size() == 1) {
                    simpleFunctionName = functionNameAsMap.get(AbstractOrganizableObject.NAME);
                }

                if (simpleFunctionName != null && !simpleFunctionName.isContainerNode()) {
                    return simpleFunctionName.asText();
                } else {
                    DynamicValue<String> dynamicValue = jsonObjectMapper.treeToValue(simpleFunctionName, DynamicValue.class);
                    if(dynamicValue != null && !dynamicValue.isDynamic()){
                        return dynamicValue.getValue();
                    } else {
                        return null;
                    }
                }
            } else {
                throw new IllegalArgumentException("Invalid function. Function selector for yaml only supports function selectors as jsons, but was: " + dynamicSelectionCriteria.getValue());
            }

        } else {
            return null;
        }
    }

    /**
     * Returns all selection criteria for keyword serialization
     */
    private Map<String, DynamicValue<String>> getFunctionSelectionCriteriaForYamlSerialization(DynamicValue<String> function, ObjectMapper jsonObjectMapper) throws JsonProcessingException {
        if (!function.getValue().trim().isEmpty()) {
            if (function.getValue().startsWith("{")) {
                Map<String, DynamicValue<String>> result = new HashMap<>();
                TypeReference<HashMap<String, JsonNode>> functionValueTypeRef = new TypeReference<>() {
                };
                HashMap<String, JsonNode> selectionCriteriaMap = jsonObjectMapper.readValue(function.getValue(), functionValueTypeRef);
                for (Map.Entry<String, JsonNode> entry : selectionCriteriaMap.entrySet()) {
                    if (entry.getValue().isContainerNode()) {
                        result.put(entry.getKey(), jsonObjectMapper.treeToValue(entry.getValue(), DynamicValue.class));
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
