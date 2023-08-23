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
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.artefacts.CallFunction;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.deserializers.YamlArtefactFieldDeserializationProcessor;
import step.plans.parser.yaml.schema.YamlDynamicValueJsonSchemaHelper;
import step.plans.parser.yaml.schema.YamlPlanJsonSchemaGenerator;
import step.plans.parser.yaml.serializers.YamlArtefactFieldSerializationProcessor;

import java.util.HashMap;
import java.util.Map;

public class KeywordSelectionRule implements ArtefactFieldConversionRule {

    protected final ObjectMapper jsonObjectMapper;

    public KeywordSelectionRule() {
        jsonObjectMapper = new ObjectMapper();
    }

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        // special syntax for 'keyword' field
        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            boolean isCallFunction = CallFunction.class.isAssignableFrom(objectClass);
            if (isCallFunction) {
                if (field.getName().equals(YamlPlanFields.CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD)) {
                    JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();
                    YamlPlanJsonSchemaGenerator.addRef(nestedPropertyParamsBuilder, YamlDynamicValueJsonSchemaHelper.SMART_DYNAMIC_VALUE_STRING_DEF);
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
            if (artefactClass.equals(CallFunction.ARTEFACT_NAME) && field.getKey().equals(YamlPlanFields.CALL_FUNCTION_FUNCTION_YAML_FIELD)) {
                JsonNode yamlFunctionValue = field.getValue();

                // for function name we need to prepare the following output:
                /*
                    "function": {
                        "dynamic": false,
                        "value": "{\"name\":{\"value\":\"MyKeyword1\",\"dynamic\":false}}"
                    }
                */
                DynamicValue<String> keywordName = getDynamicKeywordName(yamlFunctionValue);
                Map<String, DynamicValue<String>> keywordFunctionCriteria = new HashMap<>();

                // "{\"name\":{\"value\":\"MyKeyword1\",\"dynamic\":false}}"
                keywordFunctionCriteria.put(AbstractOrganizableObject.NAME, keywordName);

                DynamicValue<String> functionDynamicValue = new DynamicValue<>(jsonObjectMapper.writeValueAsString(keywordFunctionCriteria));
                output.putPOJO(YamlPlanFields.CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD, functionDynamicValue);

                return true;
            } else {
                return false;
            }
        };
    }

    private DynamicValue<String> getDynamicKeywordName(JsonNode yamlFunctionValue) {
        DynamicValue<String> keywordName;
        if(yamlFunctionValue.isContainerNode() && yamlFunctionValue.get(YamlPlanFields.DYN_VALUE_EXPRESSION_FIELD) != null){
            keywordName = new DynamicValue<>(yamlFunctionValue.get(YamlPlanFields.DYN_VALUE_EXPRESSION_FIELD).asText(), "");
        } else {
            keywordName = new DynamicValue<>(yamlFunctionValue.asText());
        }
        return keywordName;
    }

    @Override
    public YamlArtefactFieldSerializationProcessor getArtefactFieldSerializationProcessor() {
        return (artefact, field, fieldMetadata, gen) -> {
            if (CallFunction.class.isAssignableFrom(artefact.getClass())) {
                if ((field.getName().equals(YamlPlanFields.CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD))) {
                    DynamicValue<String> function = (DynamicValue<String>) field.get(artefact);

                    gen.writeFieldName(YamlPlanFields.CALL_FUNCTION_FUNCTION_YAML_FIELD);

                    DynamicValue<String> functionNameDynamicValue = getFunctionNameDynamicValue(function);

                    // here we want to write function name simply as 'keyword: "myKeyword"' in YAML
                    if (functionNameDynamicValue != null) {
                        gen.writeObject(functionNameDynamicValue);
                    }
                    return true;
                }
            }
            return false;
        };
    }

    private DynamicValue<String> getFunctionNameDynamicValue(DynamicValue<String> function) throws JsonProcessingException {
        if (function.getValue().trim().length() > 0) {
            if (function.getValue().startsWith("{")) {
                TypeReference<HashMap<String, DynamicValue<String>>> typeRef = new TypeReference<>() {
                };
                HashMap<String, DynamicValue<String>> functionNameAsMap = jsonObjectMapper.readValue(function.getValue(), typeRef);
                if (functionNameAsMap.isEmpty()) {
                    return null;
                }
                if (functionNameAsMap.size() > 1) {
                    throw new IllegalArgumentException("Invalid function. Function selector for yaml only supports search by function name, but was: " + function.getValue());
                }
                DynamicValue<String> functionNameDynamicValue = functionNameAsMap.get(AbstractOrganizableObject.NAME);
                if (functionNameDynamicValue == null) {
                    throw new IllegalArgumentException("Invalid function. Function selector for yaml only supports search by function name, but was: " + function.getValue());
                }
//                if (functionNameDynamicValue.isDynamic()) {
//                    throw new IllegalArgumentException("Invalid function. Function selector for yaml only supports static function names, but was: " + function.getValue());
//                }
                return functionNameDynamicValue;
            } else {
                throw new IllegalArgumentException("Invalid function. Function selector for yaml only supports function selectors as jsons, but was: " + function.getValue());
            }

        } else {
            return null;
        }
    }

}
