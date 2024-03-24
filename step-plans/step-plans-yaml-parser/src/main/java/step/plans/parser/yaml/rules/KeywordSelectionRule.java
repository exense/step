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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.artefacts.CallFunction;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.YamlFields;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.deserializers.YamlArtefactFieldDeserializationProcessor;
import step.plans.parser.yaml.serializers.YamlArtefactFieldSerializationProcessor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class KeywordSelectionRule implements ArtefactFieldConversionRule {

    protected final ObjectMapper jsonObjectMapper;

    public KeywordSelectionRule() {
        jsonObjectMapper = DefaultJacksonMapperProvider.getObjectMapper();
    }

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        // special syntax for 'keyword' field
        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            boolean isCallFunction = CallFunction.class.isAssignableFrom(objectClass);
            if (isCallFunction) {
                YamlJsonSchemaHelper jsonSchemaHelper = new YamlJsonSchemaHelper(jsonProvider);
                if (field.getName().equals(YamlPlanFields.CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD)) {
                    JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();
                    JsonArrayBuilder oneOfArrayBuilder = jsonProvider.createArrayBuilder();
                    oneOfArrayBuilder
                            .add(jsonProvider.createObjectBuilder().add("type", "string"))
                            .add(jsonSchemaHelper.createPatternPropertiesWithDynamicValues());
                    nestedPropertyParamsBuilder.add("oneOf", oneOfArrayBuilder);
                    propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
                    return true;
                }
            }
            return false;
        };
    }

    @Override
    public YamlArtefactFieldDeserializationProcessor getArtefactFieldDeserializationProcessor() {
        return (artefactClass, field, output, codec) -> {
            // for 'CallFunction' we can use either the `keyword` (keyword name) field or the `keyword.routing` to define the keyword name
            if (artefactClass.equals(CallFunction.ARTEFACT_NAME) && field.getKey().equals(CallFunction.CALL_FUNCTION_FUNCTION_YAML_FIELD)) {
                JsonNode yamlFunctionValue = field.getValue();

                // for function name we need to prepare the following output:
                /*
                    "function": {
                        "dynamic": false,
                        "value": "{\"name\":{\"value\":\"MyKeyword1\",\"dynamic\":false}}"
                    }
                */
                Map<String, DynamicValue<String>> criteria = getDynamicSelectionCriteriaForDeserialization(yamlFunctionValue);
                DynamicValue<String> functionDynamicValue = new DynamicValue<>(jsonObjectMapper.writeValueAsString(criteria));
                output.putPOJO(YamlPlanFields.CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD, functionDynamicValue);

                return true;
            } else {
                return false;
            }
        };
    }

    /**
     * Converts the selection criteria from yaml format to the technical format
     */
    private Map<String, DynamicValue<String>> getDynamicSelectionCriteriaForDeserialization(JsonNode yamlFunctionValue) {
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

    @Override
    public YamlArtefactFieldSerializationProcessor getArtefactFieldSerializationProcessor() {
        return (artefact, field, fieldMetadata, gen) -> {
            if (CallFunction.class.isAssignableFrom(artefact.getClass())) {
                if ((field.getName().equals(YamlPlanFields.CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD))) {
                    DynamicValue<String> function = (DynamicValue<String>) field.get(artefact);
                    gen.writeFieldName(CallFunction.CALL_FUNCTION_FUNCTION_YAML_FIELD);

                    String simpleFunctionName = getSimpleFunctionNameForYamlSerialization(function, jsonObjectMapper);
                    if (simpleFunctionName != null) {
                        gen.writeString(simpleFunctionName);
                    } else {
                        Map<String, DynamicValue<String>> selectionCriteria = getFunctionSelectionCriteriaForYamlSerialization(function, jsonObjectMapper);
                        if (selectionCriteria != null) {
                            gen.writeStartObject();
                            for (Map.Entry<String, DynamicValue<String>> entry : selectionCriteria.entrySet()) {
                                gen.writeObjectField(entry.getKey(), entry.getValue());
                            }
                            gen.writeEndObject();
                        }
                    }
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Returns function name from function selection criteria (if selection criteria by function name exists).
     */
    public static DynamicValue<String> getFunctionNameDynamicValue(DynamicValue<String> function, ObjectMapper jsonObjectMapper) throws JsonProcessingException {
        if (function.getValue().trim().length() > 0) {
            if (function.getValue().startsWith("{")) {
                TypeReference<HashMap<String, JsonNode>> functionValueTypeRef = new TypeReference<>() {
                };
                HashMap<String, JsonNode> functionNameAsMap = jsonObjectMapper.readValue(function.getValue(), functionValueTypeRef);
                if (functionNameAsMap.isEmpty()) {
                    return null;
                }
                JsonNode functionName = functionNameAsMap.get(AbstractOrganizableObject.NAME);
                if (functionName == null) {
                    return null;
                }

                // function name can be either the dynamic value or the simple string (if converted from the plain-text format)
                if (functionName.isContainerNode()) {
                    return jsonObjectMapper.treeToValue(functionName, DynamicValue.class);
                } else {
                    return new DynamicValue<>(functionName.asText());
                }
           } else {
                throw new IllegalArgumentException("Invalid function. Function selector for yaml only supports function selectors as jsons, but was: " + function.getValue());
            }

        } else {
            return null;
        }
    }

    /**
     * Returns the simple function name for yaml format (if the function contains only the 'name' criteria).
     * Returns null otherwise.
     */
    private String getSimpleFunctionNameForYamlSerialization(DynamicValue<String> function, ObjectMapper jsonObjectMapper) throws JsonProcessingException {
        if (function.getValue().trim().length() > 0) {
            if (function.getValue().startsWith("{")) {
                TypeReference<HashMap<String, JsonNode>> functionValueTypeRef = new TypeReference<>() {
                };
                HashMap<String, JsonNode> functionNameAsMap = jsonObjectMapper.readValue(function.getValue(), functionValueTypeRef);

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
                throw new IllegalArgumentException("Invalid function. Function selector for yaml only supports function selectors as jsons, but was: " + function.getValue());
            }

        } else {
            return null;
        }
    }

    /**
     * Returns all selection criteria for keyword serialization
     */
    private Map<String, DynamicValue<String>> getFunctionSelectionCriteriaForYamlSerialization(DynamicValue<String> function, ObjectMapper jsonObjectMapper) throws JsonProcessingException {
        if (function.getValue().trim().length() > 0) {
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
