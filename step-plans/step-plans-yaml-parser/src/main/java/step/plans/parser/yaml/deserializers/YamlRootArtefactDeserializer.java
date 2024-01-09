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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.artefacts.CallFunction;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;
import step.core.scanner.CachedAnnotationScanner;
import step.core.yaml.deserializers.StepYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializerAddOn;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.YamlPlanReaderExtender;
import step.plans.parser.yaml.YamlPlanReaderExtension;
import step.plans.parser.yaml.model.YamlRootArtefact;
import step.plans.parser.yaml.rules.*;
import step.core.yaml.schema.JsonSchemaFieldProcessingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static step.core.scanner.Classes.newInstanceAs;

@StepYamlDeserializerAddOn(targetClasses = {YamlRootArtefact.class})
public class YamlRootArtefactDeserializer extends StepYamlDeserializer<YamlRootArtefact> {

    private final List<YamlArtefactFieldDeserializationProcessor> customFieldProcessors;

    public YamlRootArtefactDeserializer() {
        this(null);
    }

    public YamlRootArtefactDeserializer(ObjectMapper stepYamlObjectMapper) {
        super(stepYamlObjectMapper);
        this.customFieldProcessors = prepareFieldProcessors();
    }

    protected List<YamlArtefactFieldDeserializationProcessor> prepareFieldProcessors() {
        List<YamlArtefactFieldDeserializationProcessor> res = new ArrayList<>();

        // -- BASIC PROCESSING RULES

        // the 'name' field should be wrapped into the 'attributes'
        res.add(new NodeNameRule().getArtefactFieldDeserializationProcessor());

        // process children recursively
        res.add((artefactClass, field, output, codec) -> {
            if (field.getKey().equals(YamlPlanFields.ARTEFACT_CHILDREN)) {
                JsonNode yamlChildren = field.getValue();
                if (yamlChildren != null && yamlChildren.isArray()) {
                    ArrayNode childrenResult = createArrayNode(codec);
                    for (JsonNode yamlChild : yamlChildren) {
                        childrenResult.add(convertYamlArtefact(yamlChild, codec, customFieldProcessors));
                    }
                    output.set(YamlPlanFields.ARTEFACT_CHILDREN, childrenResult);
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
        res.add(new KeywordRoutingRule().getArtefactFieldDeserializationProcessor());
        res.add(new FunctionGroupSelectionRule().getArtefactFieldDeserializationProcessor());
        res.add(new ForBlockRule(yamlObjectMapper).getArtefactFieldDeserializationProcessor());
        res.add(new ForEachBlockRule(yamlObjectMapper).getArtefactFieldDeserializationProcessor());
        res.add(new DataSetRule(yamlObjectMapper).getArtefactFieldDeserializationProcessor());

        // for 'Check' we always use the dynamic expression for 'expression' field (static value is not supported)
        res.add(new CheckExpressionRule().getArtefactFieldDeserializationProcessor());
        return res;
    }

    protected List<YamlArtefactFieldDeserializationProcessor> getExtensions() {
        List<YamlArtefactFieldDeserializationProcessor> extensions = new ArrayList<>();
        CachedAnnotationScanner.getClassesWithAnnotation(YamlPlanReaderExtension.LOCATION, YamlPlanReaderExtension.class, Thread.currentThread().getContextClassLoader()).stream()
                .map(newInstanceAs(YamlPlanReaderExtender.class)).forEach(e -> extensions.addAll(e.getDeserializationExtensions()));
        return extensions;
    }

    @Override
    public YamlRootArtefact deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        JsonNode techArtefact = convertYamlArtefact(node, jsonParser.getCodec(), customFieldProcessors);
        return new YamlRootArtefact(jsonParser.getCodec().treeToValue(techArtefact, AbstractArtefact.class));
    }

    private static JsonNode convertYamlArtefact(JsonNode yamlArtefact, ObjectCodec codec, List<YamlArtefactFieldDeserializationProcessor> customFieldProcessors) throws JsonSchemaFieldProcessingException, JsonProcessingException {
        ObjectNode techArtefact = createObjectNode(codec);

        // move artifact class into the '_class' field
        Iterator<String> childrenArtifactNames = yamlArtefact.fieldNames();

        List<String> artifactNames = new ArrayList<String>();
        childrenArtifactNames.forEachRemaining(artifactNames::add);

        String yamlArtifactClass = null;
        if (artifactNames.size() == 0) {
            throw new JsonSchemaFieldProcessingException("Artifact should have a name");
        } else if (artifactNames.size() > 1) {
            throw new JsonSchemaFieldProcessingException("Artifact should have only one name");
        } else {
            yamlArtifactClass = artifactNames.get(0);
        }

        if (yamlArtifactClass != null) {
            // java artifact has UpperCamelCase, but in Yaml we use lowerCamelCase
            String javaArtifactClass = YamlPlanFields.yamlArtefactNameToJava(yamlArtifactClass);
            JsonNode artifactData = yamlArtefact.get(yamlArtifactClass);
            techArtefact.put(Plan.JSON_CLASS_FIELD, javaArtifactClass);

            fillDefaultValuesForArtifactFields(javaArtifactClass, (ObjectNode) artifactData);

            Iterator<Map.Entry<String, JsonNode>> fields = artifactData.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> next = fields.next();

                // process some fields ('name', 'children' etc.) in special way
                boolean processedAsSpecialField = false;
                for (YamlArtefactFieldDeserializationProcessor proc : customFieldProcessors) {
                    if (proc.deserializeArtefactField(javaArtifactClass, next, techArtefact, codec)) {
                        processedAsSpecialField = true;
                    }
                }

                // copy all other fields (parameters)
                if (!processedAsSpecialField) {
                    techArtefact.set(next.getKey(), next.getValue().deepCopy());
                }
            }

        }
        return techArtefact;
    }

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
