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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;
import step.plans.simple.YamlPlanFields;
import step.plans.simple.model.SimpleRootArtefact;
import step.plans.simple.rules.FunctionGroupSelectionRule;
import step.plans.simple.rules.KeywordInputsRule;
import step.plans.simple.rules.KeywordSelectionRule;
import step.plans.simple.rules.NodeNameRule;
import step.plans.simple.schema.JsonSchemaFieldProcessingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SimpleRootArtefactDeserializer extends JsonDeserializer<SimpleRootArtefact> {

    private final List<SimpleArtefactFieldDeserializationProcessor> customFieldProcessors;

    public SimpleRootArtefactDeserializer() {
        this.customFieldProcessors = prepareFieldProcessors();
    }

    protected List<SimpleArtefactFieldDeserializationProcessor> prepareFieldProcessors() {
        List<SimpleArtefactFieldDeserializationProcessor> temp;
        temp = new ArrayList<>();

        // the 'name' field should be wrapped into the 'attributes'
        temp.add(new NodeNameRule().getArtefactFieldDeserializationProcessor());

        // process children recursively
        temp.add((artefactClass, field, output, codec) -> {
            if (field.getKey().equals("children")) {
                JsonNode simpleChildren = field.getValue();
                if (simpleChildren != null && simpleChildren.isArray()) {
                    ArrayNode childrenResult = createArrayNode(codec);
                    for (JsonNode simpleChild : simpleChildren) {
                        childrenResult.add(convertSimpleArtifactToFull(simpleChild, codec, customFieldProcessors));
                    }
                    output.set("children", childrenResult);
                }
                return true;
            } else {
                return false;
            }
        });

        // 'argument' field for 'CallKeyword' artifact should contain all input values (dynamic values) as json string
        // but in simplified format we represent input values as array of key / values
        temp.add(new KeywordInputsRule().getArtefactFieldDeserializationProcessor());

        // for 'CallFunction' we can use either the `keyword` (keyword name) field or the `keyword.selectionCriteria` to define the keyword name
        // and 'token' aka 'selectionCriteria' field should contain all input values (dynamic values) as json string
        //  but in simplified format we represent input values as array of key / values
        temp.add(new KeywordSelectionRule().getArtefactFieldDeserializationProcessor());
        temp.add(new FunctionGroupSelectionRule().getArtefactFieldDeserializationProcessor());
        return temp;
    }

    @Override
    public SimpleRootArtefact deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        JsonNode fullArtifact = convertSimpleArtifactToFull(node, jsonParser.getCodec(), customFieldProcessors);
        return new SimpleRootArtefact(jsonParser.getCodec().treeToValue(fullArtifact, AbstractArtefact.class));
    }

    private static JsonNode convertSimpleArtifactToFull(JsonNode simpleArtifact, ObjectCodec codec, List<SimpleArtefactFieldDeserializationProcessor> customFieldProcessors) throws JsonSchemaFieldProcessingException, JsonProcessingException {
        ObjectNode fullArtifact = createObjectNode(codec);

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

            fillDefaultValuesForArtifactFields(shortArtifactClass, (ObjectNode) artifactData);

            Iterator<Map.Entry<String, JsonNode>> fields = artifactData.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> next = fields.next();

                // process some fields ('name', 'children' etc.) in special way
                boolean processedAsSpecialField = false;
                for (SimpleArtefactFieldDeserializationProcessor proc : customFieldProcessors) {
                    if (proc.deserializeArtefactField(shortArtifactClass, next, fullArtifact, codec)) {
                        processedAsSpecialField = true;
                    }
                }

                // copy all other fields (parameters)
                if (!processedAsSpecialField) {
                    fullArtifact.set(next.getKey(), next.getValue().deepCopy());
                }
            }

        }
        return fullArtifact;
    }

    private static void fillDefaultValuesForArtifactFields(String shortArtifactClass, ObjectNode artifactData) {
        // name is required attribute in json schema
        JsonNode name = artifactData.get(YamlPlanFields.NAME_SIMPLE_FIELD);
        if (name == null) {
            artifactData.put(YamlPlanFields.NAME_SIMPLE_FIELD, shortArtifactClass);
        }
    }

    private static ArrayNode createArrayNode(ObjectCodec codec) {
        return (ArrayNode) codec.createArrayNode();
    }

    private static ObjectNode createObjectNode(ObjectCodec codec) {
        return (ObjectNode) codec.createObjectNode();
    }

}
