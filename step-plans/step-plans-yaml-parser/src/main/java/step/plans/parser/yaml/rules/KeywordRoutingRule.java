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
import step.artefacts.TokenSelector;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.deserializers.YamlArtefactFieldDeserializationProcessor;
import step.plans.parser.yaml.serializers.YamlArtefactFieldSerializationProcessor;

public class KeywordRoutingRule extends DynamicInputsSupport implements ArtefactFieldConversionRule {

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        // special syntax for 'keyword' field
        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            boolean isCallFunction = isCallFunction(objectClass);
            if (isCallFunction) {
                if (field.getName().equals(YamlPlanFields.TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD)) {
                    // token is renamed to 'routing' in yaml
                    JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();
                    YamlJsonSchemaHelper.addRef(nestedPropertyParamsBuilder, YamlJsonSchemaHelper.DYNAMIC_KEYWORD_INPUTS_DEF);
                    propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
                    return true;
                }
            }
            return false;
        };
    }

    private boolean isCallFunction(Class<?> objectClass) {
        return CallFunction.class.isAssignableFrom(objectClass);
    }

    @Override
    public YamlArtefactFieldDeserializationProcessor getArtefactFieldDeserializationProcessor() {
        return (artefactClass, field, output, codec) -> {
            // rename 'routing' to 'token'
            if (artefactClass.equals(CallFunction.ARTEFACT_NAME) && field.getKey().equals(TokenSelector.TOKEN_SELECTOR_TOKEN_YAML_FIELD)) {
                JsonNode functionRouting = field.getValue();

                if (functionRouting instanceof ArrayNode) {
                    output.put(YamlPlanFields.TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD, deserializeDynamicInputs(codec, (ArrayNode) functionRouting));
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
            if (isCallFunction(artefact.getClass())) {
                if ((field.getName().equals(YamlPlanFields.TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD))) {
                    DynamicValue<String> token = ((CallFunction) artefact).getToken();

                    if (isNotEmptyDynamicInputs(token)) {
                        // write token as 'routing'
                        gen.writeFieldName(TokenSelector.TOKEN_SELECTOR_TOKEN_YAML_FIELD);
                        serializeDynamicInputs(gen, token);
                    }
                    return true;
                }
            }
            return false;
        };
    }
}
