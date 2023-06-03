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
package step.plans.simple.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;
import step.plans.simple.model.SimpleRootArtefact;
import step.plans.simple.schema.JsonSchemaFieldProcessingException;

import java.io.IOException;
import java.util.*;

public class SimpleRootArtefactDeserializer extends JsonDeserializer<SimpleRootArtefact> {

    @Override
    public SimpleRootArtefact deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        JsonNode fullArtifact = convertSimpleArtifactToFull(node, jsonParser.getCodec());
        return new SimpleRootArtefact(jsonParser.getCodec().treeToValue(fullArtifact, AbstractArtefact.class));
    }

    private JsonNode convertSimpleArtifactToFull(JsonNode simpleArtifact, ObjectCodec codec) throws JsonSchemaFieldProcessingException {
        ObjectNode fullArtifact = createObjectNode(codec);

        // all fields except for 'children' and 'name' will be copied from simple artifact
        List<String> specialFields = Arrays.asList("children", "name");

        // move artifact class into the '_class' field
        Iterator<String> childrenArtifactNames = simpleArtifact.fieldNames();

        List<String> artifactNames = new ArrayList<String>();
        childrenArtifactNames.forEachRemaining(artifactNames::add);

        String shortArtifactClass = null;
        if (artifactNames.size() == 0) {
            throw new JsonSchemaFieldProcessingException("Artifact should have a name");
        } else if (artifactNames.size() > 1) {
            throw new JsonSchemaFieldProcessingException("Artifact should have only one name");
        } else {
            shortArtifactClass = artifactNames.get(0);
        }

        if (shortArtifactClass != null) {
            JsonNode artifactData = simpleArtifact.get(shortArtifactClass);
            fullArtifact.put(Plan.JSON_CLASS_FIELD, shortArtifactClass);

            // the 'name' field is NOT wrapped into the 'attributes'
            ObjectNode planAttributesNode = createObjectNode(codec);

            // name is required attribute in json schema
            JsonNode name = artifactData.get("name");
            if (name == null) {
                throw new JsonSchemaFieldProcessingException("'name' attribute is not defined for artifact " + shortArtifactClass);
            }

            planAttributesNode.put("name", name.asText());
            fullArtifact.set("attributes", planAttributesNode);

            // copy all other fields (parameters)
            Iterator<Map.Entry<String, JsonNode>> fields = artifactData.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> next = fields.next();
                if (!specialFields.contains(next.getKey())) {
                    fullArtifact.set(next.getKey(), next.getValue().deepCopy());
                }
            }

            // process children recursively
            JsonNode simpleChildren = artifactData.get("children");
            if (simpleChildren != null && simpleChildren.isArray()) {
                ArrayNode childrenResult = createArrayNode(codec);
                for (JsonNode simpleChild : simpleChildren) {
                    childrenResult.add(convertSimpleArtifactToFull(simpleChild, codec));
                }
                fullArtifact.set("children", childrenResult);
            }
        }
        return fullArtifact;
    }

    private static ArrayNode createArrayNode(ObjectCodec codec) {
        return (ArrayNode) codec.createArrayNode();
    }

    private ObjectNode createObjectNode(ObjectCodec codec) {
        return (ObjectNode) codec.createObjectNode();
    }

}
