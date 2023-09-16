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
package step.automation.packages.yaml.deserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.automation.packages.yaml.YamlKeywordsLookuper;
import step.automation.packages.yaml.model.AutomationPackageKeyword;
import step.automation.packages.yaml.model.AutomationPackageKeywords;
import step.automation.packages.yaml.rules.KeywordNameRule;
import step.automation.packages.yaml.rules.TokenSelectionCriteriaRule;
import step.automation.packages.yaml.rules.YamlKeywordConversionRule;
import step.functions.Function;

import java.io.IOException;
import java.util.*;

public class YamlKeywordsDeserializer extends JsonDeserializer<AutomationPackageKeywords> {

    private final YamlKeywordsLookuper keywordsLookuper;

    public YamlKeywordsDeserializer() {
        this.keywordsLookuper = new YamlKeywordsLookuper();
    }

    @Override
    public AutomationPackageKeywords deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        List<AutomationPackageKeyword> functions = new ArrayList<>();
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode keywordNode : arrayNode) {
                String yamlKeywordClass = resolveKeywordClass(keywordNode);

                if (yamlKeywordClass != null) {
                    ObjectNode techKeyword = createObjectNode(jsonParser.getCodec());

                    // java artifact has UpperCamelCase, but in Yaml we use lowerCamelCase
                    String javaKeywordClass = keywordsLookuper.yamlKeywordClassToJava(yamlKeywordClass);
                    if (javaKeywordClass == null) {
                        throw new RuntimeException("Unable to resolve implementation class for keyword " + yamlKeywordClass);
                    }

                    JsonNode keywordFields = keywordNode.get(yamlKeywordClass);
                    techKeyword.put(Function.JSON_CLASS_FIELD, javaKeywordClass);

                    Iterator<Map.Entry<String, JsonNode>> fields = keywordFields.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> next = fields.next();

                        // process some fields in special way
                        boolean processedAsSpecialField = false;
                        for (YamlKeywordFieldDeserializationProcessor proc : deserializationProcessors()) {
                            if (proc.deserializeKeywordField(javaKeywordClass, next, techKeyword, jsonParser.getCodec())) {
                                processedAsSpecialField = true;
                            }
                        }

                        // copy all other fields (parameters)
                        if (!processedAsSpecialField) {
                            techKeyword.set(next.getKey(), next.getValue().deepCopy());
                        }
                    }

                    Function keyword = jsonParser.getCodec().treeToValue(techKeyword, Function.class);
                    Map<String, Object> specialAttributes = extractSpecialKeywordAttributes(keywordFields, keyword);
                    functions.add(new AutomationPackageKeyword(keyword, specialAttributes));
                }
            }
        }
        return new AutomationPackageKeywords(functions);
    }

    protected Map<String, Object> extractSpecialKeywordAttributes(JsonNode keywordFields, Function keyword){
        Map<String, Object> specialAttributes = new HashMap<>();

        List<SpecialKeywordAttributesExtractor> attributesExtractors = attributesExtractors(keyword);
        for (SpecialKeywordAttributesExtractor attributesExtractor : attributesExtractors) {
            attributesExtractor.extractSpecialAttributes(keywordFields, keyword, specialAttributes);
        }

        return specialAttributes;
    }

    private List<SpecialKeywordAttributesExtractor> attributesExtractors(Function keyword) {
        List<SpecialKeywordAttributesExtractor> attributesExtractors = new ArrayList<>();
        List<YamlKeywordConversionRule> conversionRulesForKeyword = keywordsLookuper.getConversionRulesForKeyword(keyword);
        for (YamlKeywordConversionRule rule : conversionRulesForKeyword) {
            SpecialKeywordAttributesExtractor extractor = rule.getSpecialAttributesExtractor();
            if(extractor != null){
                attributesExtractors.add(extractor);
            }
        }
        return attributesExtractors;
    }

    private String resolveKeywordClass(JsonNode yamlKeyword) {
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

        return yamlKeywordClass;
    }

    private List<YamlKeywordFieldDeserializationProcessor> deserializationProcessors() {
        List<YamlKeywordFieldDeserializationProcessor> processors = new ArrayList<>();

        // default rules
        // TODO: apply default keyword name in case of missing value in yaml!!!
        processors.add(new KeywordNameRule().getDeserializationProcessor());
        processors.add(new TokenSelectionCriteriaRule().getDeserializationProcessor());

        List<YamlKeywordConversionRule> additionalRules = keywordsLookuper.getAllConversionRules();
        for (YamlKeywordConversionRule rule : additionalRules) {
            YamlKeywordFieldDeserializationProcessor deserializationProcessor = rule.getDeserializationProcessor();
            if (deserializationProcessor != null) {
                processors.add(deserializationProcessor);
            }
        }
        return processors;
    }

    private static ArrayNode createArrayNode(ObjectCodec codec) {
        return (ArrayNode) codec.createArrayNode();
    }

    private static ObjectNode createObjectNode(ObjectCodec codec) {
        return (ObjectNode) codec.createObjectNode();
    }
}
