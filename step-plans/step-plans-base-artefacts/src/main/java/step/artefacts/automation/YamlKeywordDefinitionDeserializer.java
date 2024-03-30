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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.YamlFields;
import step.core.yaml.deserializers.StepYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializerAddOn;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@StepYamlDeserializerAddOn(targetClasses = {YamlKeywordDefinition.class})
public class YamlKeywordDefinitionDeserializer extends StepYamlDeserializer<YamlKeywordDefinition> {

    private final ObjectMapper simpleObjectMapper = DefaultJacksonMapperProvider.getObjectMapper();

    public YamlKeywordDefinitionDeserializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }

    @Override
    public YamlKeywordDefinition deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        Map<String, DynamicValue<String>> criteria = getDynamicSelectionCriteria(node);
        String selectionCriteriaJson = simpleObjectMapper.writeValueAsString(criteria);

        if (!node.isContainerNode()) {
            // for simple function definition the node (string node) contains explicit function name
            return new YamlKeywordDefinition(node.asText(), node.asText(), selectionCriteriaJson);
        } else {
            return new YamlKeywordDefinition(YamlKeywordDefinitionSerializer.getFunctionName(node.asText(), simpleObjectMapper, false), null, selectionCriteriaJson);
        }
    }

    private Map<String, DynamicValue<String>> getDynamicSelectionCriteria(JsonNode yamlFunctionValue) {
        // in yaml format we can simply define function name as string,
        // or we can define several selection criteria within the 'keyword' block in yaml
        if (yamlFunctionValue.isContainerNode()) {
            Map<String, DynamicValue<String>> result = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fieldsIterator = yamlFunctionValue.fields();
            while (fieldsIterator.hasNext()) {
                Map.Entry<String, JsonNode> field = fieldsIterator.next();
                if (field.getValue().isContainerNode() && field.getValue().get(YamlFields.DYN_VALUE_EXPRESSION_FIELD) != null) {
                    // selection criteria with dynamic value
                    result.put(field.getKey(), new DynamicValue<>(field.getValue().get(YamlFields.DYN_VALUE_EXPRESSION_FIELD).asText(), ""));
                } else {
                    // selection criteria with simple value
                    result.put(field.getKey(), new DynamicValue<>(field.getValue().asText()));
                }
            }
            return result;
        } else {
            // simple function name
            return Map.of(AbstractOrganizableObject.NAME, new DynamicValue<>(yamlFunctionValue.asText()));
        }
    }

}
