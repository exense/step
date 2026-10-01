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

@StepYamlSerializerAddOn(targetClasses = {YamlCallNamedEntityDefinition.class})
public class YamlCallNamedEntityDefinitionSerializer extends StepYamlSerializer<YamlCallNamedEntityDefinition> {

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = DefaultJacksonMapperProvider.getObjectMapper();

    public YamlCallNamedEntityDefinitionSerializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }

    @Override
    public void serialize(YamlCallNamedEntityDefinition value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        DynamicValue<String> dynamicValue = value.toDynamicValue();
        String simpleEntityName = getEntityName(dynamicValue, true);
        if (simpleEntityName != null) {
            gen.writeString(simpleEntityName);
        } else {
            Map<String, DynamicValue<String>> selectionCriteria = getSelectionCriteriaForYamlSerialization(dynamicValue);
            if (selectionCriteria != null) {
                gen.writeStartObject();
                for (Map.Entry<String, DynamicValue<String>> entry : selectionCriteria.entrySet()) {
                    gen.writeObjectField(entry.getKey(), entry.getValue());
                }
                gen.writeEndObject();
            }
        }
    }

    public static String getEntityName(DynamicValue<String> dynamicSelectionCriteria, boolean asSimpleEntityName) throws JsonProcessingException {
        if (!dynamicSelectionCriteria.getValue().trim().isEmpty()) {
            String jsonValue = dynamicSelectionCriteria.getValue();
            return getEntityName(jsonValue, asSimpleEntityName);
        } else {
            return null;
        }
    }

    public static String getEntityName(String jsonValue, boolean asSimpleEntityName) throws JsonProcessingException {
        if (jsonValue == null || jsonValue.isEmpty()) {
            return null;
        }
        if (jsonValue.startsWith("{")) {
            TypeReference<HashMap<String, JsonNode>> entityValueTypeRef = new TypeReference<>() {
            };
            HashMap<String, JsonNode> entityNameAsMap = DEFAULT_OBJECT_MAPPER.readValue(jsonValue, entityValueTypeRef);

            JsonNode simpleEntityName = null;
            if (!asSimpleEntityName || entityNameAsMap.size() == 1) {
                simpleEntityName = entityNameAsMap.get(AbstractOrganizableObject.NAME);
            }

            if (simpleEntityName != null && !simpleEntityName.isContainerNode()) {
                return simpleEntityName.asText();
            } else {
                DynamicValue<String> dynamicValue = DEFAULT_OBJECT_MAPPER.treeToValue(simpleEntityName, DynamicValue.class);
                if (dynamicValue != null && !dynamicValue.isDynamic()) {
                    return dynamicValue.getValue();
                } else {
                    return null;
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid Entity. Entity selector for yaml only supports entity selectors as jsons, but was: " + jsonValue);
        }
    }

    /**
     * Returns all selection criteria for keyword serialization
     */
    private Map<String, DynamicValue<String>> getSelectionCriteriaForYamlSerialization(DynamicValue<String> entity) throws JsonProcessingException {
        if (!entity.getValue().trim().isEmpty()) {
            if (entity.getValue().startsWith("{")) {
                Map<String, DynamicValue<String>> result = new HashMap<>();
                TypeReference<HashMap<String, JsonNode>> functionValueTypeRef = new TypeReference<>() {
                };
                HashMap<String, JsonNode> selectionCriteriaMap = YamlCallNamedEntityDefinitionSerializer.DEFAULT_OBJECT_MAPPER.readValue(entity.getValue(), functionValueTypeRef);
                for (Map.Entry<String, JsonNode> entry : selectionCriteriaMap.entrySet()) {
                    if (entry.getValue().isContainerNode()) {
                        result.put(entry.getKey(), YamlCallNamedEntityDefinitionSerializer.DEFAULT_OBJECT_MAPPER.treeToValue(entry.getValue(), DynamicValue.class));
                    } else {
                        result.put(entry.getKey(), new DynamicValue<>(entry.getValue().asText()));
                    }
                }
                return result;
            } else {
                throw new IllegalArgumentException("Invalid entity. Entity selector for yaml only supports entity selectors as jsons, but was: " + entity.getValue());
            }

        } else {
            return null;
        }
    }
}
