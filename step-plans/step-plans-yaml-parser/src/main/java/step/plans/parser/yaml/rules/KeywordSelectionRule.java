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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.artefacts.CallFunction;
import step.core.dynamicbeans.DynamicValue;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.deserializers.YamlArtefactFieldDeserializationProcessor;
import step.plans.parser.yaml.schema.YamlPlanJsonSchemaGenerator;
import step.plans.parser.yaml.serializers.YamlArtefactFieldSerializationProcessor;

public class KeywordSelectionRule extends DynamicInputsSupport implements ArtefactFieldConversionRule {

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        // combines 'keyword' (keyword name) and 'selection' criteria
        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            boolean isCallFunction = CallFunction.class.isAssignableFrom(objectClass);
            if (isCallFunction) {
                if (field.getName().equals(YamlPlanFields.CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD)) {
                    JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();
                    YamlPlanJsonSchemaGenerator.addRef(nestedPropertyParamsBuilder, YamlPlanJsonSchemaGenerator.CALL_KEYWORD_FUNCTION_NAME_DEF);
                    propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
                    return true;
                } else if (field.getName().equals(YamlPlanFields.TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD)) {
                    // token is included in 'sectionCriteria' in 'keyword' -> just skip it in schema
                    return true;
                }
            }
            return false;
        };
    }

    @Override
    public YamlArtefactFieldDeserializationProcessor getArtefactFieldDeserializationProcessor() {
        return (artefactClass, field, output, codec) -> {
            // for 'CallFunction' we can use either the `keyword` (keyword name) field or the `keyword.selectionCriteria` to define the keyword name
            if (artefactClass.equals(CallFunction.ARTEFACT_NAME) && field.getKey().equals(YamlPlanFields.CALL_FUNCTION_FUNCTION_YAML_FIELD)) {
                JsonNode yamlFunctionValue = field.getValue();
                JsonNode functionSelectionCriteria = yamlFunctionValue.get(YamlPlanFields.TOKEN_SELECTOR_TOKEN_YAML_FIELD);

                // explicit function name as dynamic value
                if (functionSelectionCriteria != null) {
                    output.put(YamlPlanFields.TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD, deserializeDynamicInputs(codec, (ArrayNode) functionSelectionCriteria));
                } else {
                    output.set(YamlPlanFields.CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD, yamlFunctionValue);
                }
                return true;
            } else {
                return false;
            }
        };
    }

    @Override
    public YamlArtefactFieldSerializationProcessor getArtefactFieldSerializationProcessor() {
        return (artefact, field, fieldMetadata, gen) -> {
            if (CallFunction.class.isAssignableFrom(artefact.getClass())) {
                if ((field.getName().equals(YamlPlanFields.CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD))) {
                    DynamicValue<String> function = (DynamicValue<String>) field.get(artefact);
                    DynamicValue<String> token = ((CallFunction) artefact).getToken();

                    boolean useSelectionCriteria = isEmptyDynamicInputs(token);

                    gen.writeFieldName(YamlPlanFields.CALL_FUNCTION_FUNCTION_YAML_FIELD);
                    if (!useSelectionCriteria) {
                        gen.writeObject(function);
                    } else {
                        gen.writeStartObject();
                        gen.writeFieldName(YamlPlanFields.TOKEN_SELECTOR_TOKEN_YAML_FIELD);
                        serializeDynamicInputs(gen, token);
                        gen.writeEndObject();
                    }
                    return true;
                } else if (field.getName().equals(YamlPlanFields.TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD)) {
                    // token is analyzed together with 'function' field
                    return true;
                }

            }
            return false;
        };
    }

}
