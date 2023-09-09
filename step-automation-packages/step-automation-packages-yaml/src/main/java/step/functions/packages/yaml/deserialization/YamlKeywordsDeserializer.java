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
package step.functions.packages.yaml.deserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.reflections.Reflections;
import step.functions.Function;
import step.functions.packages.yaml.YamlKeywordFields;
import step.functions.packages.yaml.model.AutomationPackageKeyword;
import step.functions.packages.yaml.model.AutomationPackageKeywords;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YamlKeywordsDeserializer extends JsonDeserializer<AutomationPackageKeywords> {

    private File unzippedAutomationPackage;
    private Reflections functionReflections;

    public YamlKeywordsDeserializer(File unzippedAutomationPackage, Reflections functionReflections) {
        this.unzippedAutomationPackage = unzippedAutomationPackage;
        this.functionReflections = functionReflections;
    }

    @Override
    public AutomationPackageKeywords deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        List<AutomationPackageKeyword> functions = new ArrayList<>();
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode keywordNode : arrayNode) {
                JsonNode techKeyword = convertToTechKeyword(keywordNode, jsonParser.getCodec(), deserializationProcessors());
                Function keyword = jsonParser.getCodec().treeToValue(techKeyword, Function.class);
                Map<String, Object> specialAttributes = extractSpecialKeywordAttributes(node, keyword);
                functions.add(new AutomationPackageKeyword(keyword, specialAttributes));
            }
        }
        return new AutomationPackageKeywords(functions);
    }

    protected Map<String, Object> extractSpecialKeywordAttributes(JsonNode yamlKeyword, Function keyword){
        Map<String, Object> specialAttributes = new HashMap<>();

        // TODO: find registered attributes extractors
        List<SpecialKeywordAttributesExtractor> attributesExtractors = new ArrayList<>();
        for (SpecialKeywordAttributesExtractor attributesExtractor : attributesExtractors) {
            attributesExtractor.extractSpecialAttributes(yamlKeyword, keyword, specialAttributes);
        }

        return specialAttributes;
    }

    private JsonNode convertToTechKeyword(JsonNode yamlKeyword, ObjectCodec codec, List<YamlKeywordFieldDeserializationProcessor> customFieldProcessors) throws JsonProcessingException {
        ObjectNode techKeyword = createObjectNode(codec);

        // move keyword class into the '_class' field
        Iterator<String> childrenArtifactNames = yamlKeyword.fieldNames();

        List<String> keywordNames = new ArrayList<String>();
        childrenArtifactNames.forEachRemaining(keywordNames::add);

        String yamlKeywordClass = null;
        if (keywordNames.size() == 0) {
            throw new RuntimeException("Keyword should have a name");
        } else if (keywordNames.size() > 1) {
            throw new RuntimeException("Keyword should have only one name");
        } else {
            yamlKeywordClass = keywordNames.get(0);
        }

        if (yamlKeywordClass != null) {
            // java artifact has UpperCamelCase, but in Yaml we use lowerCamelCase
            String javaKeywordClass = YamlKeywordFields.yamlKeywordClassToJava(yamlKeywordClass, functionReflections);

            if(javaKeywordClass == null){
                throw new RuntimeException("Unable to resolve implementation class for keyword " + yamlKeywordClass);
            }

            JsonNode keywordFields = yamlKeyword.get(yamlKeywordClass);
            techKeyword.put(Function.JSON_CLASS_FIELD, javaKeywordClass);

            Iterator<Map.Entry<String, JsonNode>> fields = keywordFields.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> next = fields.next();

                // process some fields in special way
                boolean processedAsSpecialField = false;
                for (YamlKeywordFieldDeserializationProcessor proc : customFieldProcessors) {
                    if (proc.deserializeKeywordField(javaKeywordClass, next, techKeyword, codec)) {
                        processedAsSpecialField = true;
                    }
                }

                // copy all other fields (parameters)
                if (!processedAsSpecialField) {
                    techKeyword.set(next.getKey(), next.getValue().deepCopy());
                }
            }

        }
        return techKeyword;
    }

    private List<YamlKeywordFieldDeserializationProcessor> deserializationProcessors(){
        List<YamlKeywordFieldDeserializationProcessor> processors = new ArrayList<>();
        return processors;
    }

    private static ArrayNode createArrayNode(ObjectCodec codec) {
        return (ArrayNode) codec.createArrayNode();
    }

    private static ObjectNode createObjectNode(ObjectCodec codec) {
        return (ObjectNode) codec.createObjectNode();
    }
}
