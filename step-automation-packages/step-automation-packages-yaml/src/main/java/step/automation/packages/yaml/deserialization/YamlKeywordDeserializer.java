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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.yaml.AutomationPackageKeywordsLookuper;
import step.automation.packages.yaml.rules.YamlKeywordConversionRule;
import step.automation.packages.yaml.rules.keywords.KeywordNameRule;
import step.automation.packages.yaml.rules.keywords.TokenSelectionCriteriaRule;
import step.core.yaml.deserializers.NamedEntityYamlDeserializer;
import step.core.yaml.deserializers.YamlFieldDeserializationProcessor;
import step.functions.Function;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YamlKeywordDeserializer extends JsonDeserializer<AutomationPackageKeyword> {

    private final AutomationPackageKeywordsLookuper keywordsLookuper;

    public YamlKeywordDeserializer() {
        this.keywordsLookuper = new AutomationPackageKeywordsLookuper();
    }

    @Override
    public AutomationPackageKeyword deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        NamedEntityYamlDeserializer<Function> nameEntityDeserializer = new NamedEntityYamlDeserializer<>(Function.class) {
            @Override
            protected String resolveTargetClassNameByYamlName(String yamlName) {
                return keywordsLookuper.yamlKeywordClassToJava(yamlName);
            }

            @Override
            protected List<YamlFieldDeserializationProcessor> deserializationProcessors() {
                List<YamlFieldDeserializationProcessor> processors = new ArrayList<>();

                // default rules
                processors.add(new KeywordNameRule().getDeserializationProcessor());
                processors.add(new TokenSelectionCriteriaRule().getDeserializationProcessor());

                List<YamlKeywordConversionRule> additionalRules = keywordsLookuper.getAllConversionRules();
                for (YamlKeywordConversionRule rule : additionalRules) {
                    YamlFieldDeserializationProcessor deserializationProcessor = rule.getDeserializationProcessor();
                    if (deserializationProcessor != null) {
                        processors.add(deserializationProcessor);
                    }
                }
                return processors;
            }

            @Override
            protected String getTargetClassField() {
                return Function.JSON_CLASS_FIELD;
            }
        };
        Function keyword = nameEntityDeserializer.deserialize(node, jsonParser.getCodec());
        Map<String, Object> specialAttributes = extractSpecialKeywordAttributes(nameEntityDeserializer.getAllYamlFields(node), keyword);
        return new AutomationPackageKeyword(keyword, specialAttributes);
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

}
