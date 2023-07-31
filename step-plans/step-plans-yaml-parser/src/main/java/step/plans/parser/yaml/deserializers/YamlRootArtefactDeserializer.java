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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;
import step.core.scanner.CachedAnnotationScanner;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.model.YamlRootArtefact;
import step.plans.parser.yaml.rules.*;
import step.plans.parser.yaml.schema.JsonSchemaFieldProcessingException;
import step.plans.parser.yaml.YamlPlanSerializerExtender;
import step.plans.parser.yaml.YamlPlanSerializerExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static step.core.scanner.Classes.newInstanceAs;

public class YamlRootArtefactDeserializer extends JsonDeserializer<YamlRootArtefact> {

    private final List<YamlArtefactFieldDeserializationProcessor> customFieldProcessors;

    public YamlRootArtefactDeserializer() {
        this.customFieldProcessors = prepareFieldProcessors();
    }

    protected List<YamlArtefactFieldDeserializationProcessor> prepareFieldProcessors() {
        List<YamlArtefactFieldDeserializationProcessor> res = new ArrayList<>();

        // -- BASIC PROCESSING RULES

        // the 'name' field should be wrapped into the 'attributes'
        res.add(new NodeNameRule().getArtefactFieldDeserializationProcessor());

        // process children recursively
        res.add((artefactClass, field, output, codec) -> {
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

        // -- RULES FROM EXTENSIONS HAVE LESS PRIORITY THAN BASIC RULES, BUT MORE PRIORITY THAN OTHER RULES
        res.addAll(getExtensions());

        // -- RULES FOR OS ARTEFACTS

        // 'argument' field for 'CallKeyword' artifact should contain all input values (dynamic values) as json string
        // but in simplified format we represent input values as array of key / values
        res.add(new KeywordInputsRule().getArtefactFieldDeserializationProcessor());

        // for 'CallFunction' we can use either the `keyword` (keyword name) field or the `keyword.selectionCriteria` to define the keyword name
        // and 'token' aka 'selectionCriteria' field should contain all input values (dynamic values) as json string
        //  but in simplified format we represent input values as array of key / values
        res.add(new KeywordSelectionRule().getArtefactFieldDeserializationProcessor());
        res.add(new FunctionGroupSelectionRule().getArtefactFieldDeserializationProcessor());

        // for 'Check' we always use the dynamic expression for 'expression' field (static value is not supported)
        res.add(new CheckExpressionRule().getArtefactFieldDeserializationProcessor());
        return res;
    }

    protected List<YamlArtefactFieldDeserializationProcessor> getExtensions() {
        List<YamlArtefactFieldDeserializationProcessor> extensions = new ArrayList<>();
        CachedAnnotationScanner.getClassesWithAnnotation(YamlPlanSerializerExtension.class).stream()
                .map(newInstanceAs(YamlPlanSerializerExtender.class)).forEach(e -> extensions.addAll(e.getDeserializationExtensions()));
        return extensions;
    }

    @Override
    public YamlRootArtefact deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        JsonNode fullArtifact = convertSimpleArtifactToFull(node, jsonParser.getCodec(), customFieldProcessors);
        return new YamlRootArtefact(jsonParser.getCodec().treeToValue(fullArtifact, AbstractArtefact.class));
    }

    private static JsonNode convertSimpleArtifactToFull(JsonNode simpleArtifact, ObjectCodec codec, List<YamlArtefactFieldDeserializationProcessor> customFieldProcessors) throws JsonSchemaFieldProcessingException, JsonProcessingException {
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
                for (YamlArtefactFieldDeserializationProcessor proc : customFieldProcessors) {
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
