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
package step.plans.parser.yaml.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.artefacts.CallFunction;
import step.core.yaml.deserializers.StepYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializerAddOn;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.model.YamlRootArtefact;

import java.io.IOException;

@StepYamlDeserializerAddOn(targetClasses = {YamlRootArtefact.class})
public class YamlRootArtefactDeserializer extends StepYamlDeserializer<YamlRootArtefact> {

    public YamlRootArtefactDeserializer() {
        this(null);
    }

    public YamlRootArtefactDeserializer(ObjectMapper stepYamlObjectMapper) {
        super(stepYamlObjectMapper);
    }

    @Override
    public YamlRootArtefact deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
       throw new UnsupportedOperationException("Not implemented yet");
    }

    // TODO: move this logic
    private static void fillDefaultValuesForArtifactFields(String javaArtifactClass, ObjectNode artifactData) {
        // if artefact name (nodeName) is not defined in YAML, we use the artefact class as default value
        // but for CallFunction we use the keyword name as default
        JsonNode name = artifactData.get(YamlPlanFields.NAME_YAML_FIELD);
        if (name == null) {
            if(!javaArtifactClass.equals(CallFunction.ARTEFACT_NAME)) {
                artifactData.put(YamlPlanFields.NAME_YAML_FIELD, javaArtifactClass);
            } else {
                JsonNode functionNode = artifactData.get(YamlPlanFields.CALL_FUNCTION_FUNCTION_YAML_FIELD);
                if(functionNode != null && !functionNode.isContainerNode()){
                    String staticFunctionName = functionNode.asText();
                    if(!staticFunctionName.isEmpty()){
                        artifactData.put(YamlPlanFields.NAME_YAML_FIELD, staticFunctionName);
                    } else {
                        artifactData.put(YamlPlanFields.NAME_YAML_FIELD, javaArtifactClass);
                    }
                }
            }
        }
    }

    private static ArrayNode createArrayNode(ObjectCodec codec) {
        return (ArrayNode) codec.createArrayNode();
    }

    private static ObjectNode createObjectNode(ObjectCodec codec) {
        return (ObjectNode) codec.createObjectNode();
    }

}
