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
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.spi.JsonProvider;
import step.artefacts.CallFunction;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.deserializers.YamlArtefactFieldDeserializationProcessor;
import step.plans.parser.yaml.serializers.YamlArtefactFieldSerializationProcessor;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

public class NodeNameRule implements ArtefactFieldConversionRule {

    protected final ObjectMapper jsonObjectMapper = DefaultJacksonMapperProvider.getObjectMapper();

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            // TODO: potentially we want to extract mode fields from 'attributes'..
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

    public static String defaultNodeName(AbstractArtefact artefact, ObjectMapper jsonObjectMapper) throws JsonProcessingException {
        if (artefact instanceof CallFunction) {
            // for CallFunction the default name is function name
            DynamicValue<String> function = ((CallFunction) artefact).getFunction();
            if (function != null) {
                DynamicValue<String> functionName = KeywordSelectionRule.getFunctionNameDynamicValue(function, jsonObjectMapper);
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

    @Override
    public YamlArtefactFieldSerializationProcessor getArtefactFieldSerializationProcessor() {
        return (artefact, field, fieldMetadata, gen) -> {
            if (isAttributesField(field)) {
                Map<String, String> attributes = (Map<String, String>) field.get(artefact);
                String name = attributes.get(AbstractArtefact.NAME);

                // don't serialize default artefact name to YAML
                if (!Objects.equals(name, defaultNodeName(artefact, jsonObjectMapper))) {
                    gen.writeStringField(YamlPlanFields.NAME_YAML_FIELD, name);
                }
                return true;
            }
            return false;
        };
    }

    @Override
    public YamlArtefactFieldDeserializationProcessor getArtefactFieldDeserializationProcessor() {
        return new YamlArtefactFieldDeserializationProcessor() {
            @Override
            public boolean deserializeArtefactField(String artefactClass, Map.Entry<String, JsonNode> field, ObjectNode output, ObjectCodec codec) {
                if (field.getKey().equals(YamlPlanFields.NAME_YAML_FIELD)) {
                    ObjectNode attributesNode = (ObjectNode) output.get("attributes");
                    if (attributesNode == null) {
                        attributesNode = createObjectNode(codec);
                    }
                    attributesNode.put(AbstractArtefact.NAME, field.getValue().asText());
                    output.set("attributes", attributesNode);
                    return true;
                } else {
                    return false;
                }
            }
        };
    }

    private boolean isAttributesField(Field field) {
        return field.getDeclaringClass().equals(AbstractOrganizableObject.class) && field.getName().equals("attributes");
    }

}