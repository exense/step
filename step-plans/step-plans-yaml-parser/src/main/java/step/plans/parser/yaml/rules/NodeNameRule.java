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
import jakarta.json.spi.JsonProvider;
import step.artefacts.CallFunction;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.parser.yaml.YamlPlanFields;

import java.lang.reflect.Field;
import java.util.HashMap;

public class NodeNameRule implements ArtefactFieldConversionRule {

    protected final ObjectMapper jsonObjectMapper = DefaultJacksonMapperProvider.getObjectMapper();

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

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            if (isAttributesField(field)) {
                // use artefact name as default
                propertiesBuilder.add(
                        YamlPlanFields.NAME_YAML_FIELD,
                        jsonProvider.createObjectBuilder()
                                .add("type", "string")
                                .add("default", artefactClassNodeName((Class<? extends AbstractArtefact>) objectClass))
                );
                return true;
            } else {
                return false;
            }
        };
    }

    // TODO: remove this code
    public static String defaultNodeName(AbstractArtefact artefact, ObjectMapper jsonObjectMapper) throws JsonProcessingException {
        if (artefact instanceof CallFunction) {
            // for CallFunction the default name is function name
            DynamicValue<String> function = ((CallFunction) artefact).getFunction();
            if (function != null) {
                DynamicValue<String> functionName = getFunctionNameDynamicValue(function, jsonObjectMapper);
                if (functionName != null && !functionName.isDynamic()) {
                    return functionName.getValue();
                }
            }
        }
        return artefactClassNodeName(artefact.getClass());
    }

    private static String artefactClassNodeName(Class<? extends AbstractArtefact> artefactClass) {
        return AbstractArtefact.getArtefactName(artefactClass);
    }


    private boolean isAttributesField(Field field) {
        return field.getDeclaringClass().equals(AbstractOrganizableObject.class) && field.getName().equals("attributes");
    }

}
