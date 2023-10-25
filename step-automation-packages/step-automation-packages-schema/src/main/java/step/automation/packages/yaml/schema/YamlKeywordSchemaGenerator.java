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
package step.automation.packages.yaml.schema;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.yaml.YamlKeywordsLookuper;
import step.automation.packages.yaml.rules.TechnicalFieldRule;
import step.automation.packages.yaml.rules.TokenSelectionCriteriaRule;
import step.core.yaml.schema.*;
import step.functions.Function;
import step.handlers.javahandler.jsonschema.*;

import java.util.*;
import java.util.stream.Collectors;

public class YamlKeywordSchemaGenerator {

    private static final Logger log = LoggerFactory.getLogger(YamlKeywordSchemaGenerator.class);

    public static final String KEYWORD_DEF = "KeywordDef";

    private final jakarta.json.spi.JsonProvider jsonProvider;

    protected final YamlJsonSchemaHelper schemaHelper;

    protected final YamlKeywordsLookuper yamlKeywordsLookuper = new YamlKeywordsLookuper();
    private final JsonSchemaCreator jsonSchemaCreator;

    public YamlKeywordSchemaGenerator(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
        this.schemaHelper = new YamlJsonSchemaHelper(jsonProvider);

        // --- Fields metadata rules (fields we want to rename)
        FieldMetadataExtractor fieldMetadataExtractor = prepareMetadataExtractor();

        List<JsonSchemaFieldProcessor> processingRules = prepareFieldProcessors();
        this.jsonSchemaCreator = new JsonSchemaCreator(jsonProvider, new AggregatedJsonSchemaFieldProcessor(processingRules), fieldMetadataExtractor);
    }

    /**
     * Prepares definitions to be reused in json subschemas
     */
    public JsonObjectBuilder createKeywordDefs() throws JsonSchemaPreparationException {
        JsonObjectBuilder defsBuilder = jsonProvider.createObjectBuilder();

        List<JsonSchemaDefinitionCreator> definitionCreators = new ArrayList<>();

        // prepare definitions for generic DynamicValue class
        definitionCreators.add((defsList) -> {
            Map<String, JsonObjectBuilder> dynamicValueDefs = schemaHelper.createDynamicValueImplDefs();
            for (Map.Entry<String, JsonObjectBuilder> dynamicValueDef : dynamicValueDefs.entrySet()) {
                defsBuilder.add(dynamicValueDef.getKey(), dynamicValueDef.getValue());
            }
        });

        // prepare definitions for subclasses annotated with @Artefact
        definitionCreators.add((defsList) -> {
            Map<String, JsonObjectBuilder> keywordImplDefs = createKeywordImplDefs();
            for (Map.Entry<String, JsonObjectBuilder> artefactImplDef : keywordImplDefs.entrySet()) {
                defsBuilder.add(artefactImplDef.getKey(), artefactImplDef.getValue());
            }

            // add definition for "anyOf" artefact definitions prepared above
            defsBuilder.add(KEYWORD_DEF, createKeywordDef(keywordImplDefs.keySet()));
        });

        // add definitions from extensions (additional definitions for EE artefacts)
        definitionCreators.addAll(getDefinitionsExtensions());

        for (JsonSchemaDefinitionCreator definitionCreator : definitionCreators) {
            definitionCreator.addDefinition(defsBuilder);
        }

        return defsBuilder;
    }



    protected JsonObjectBuilder createKeywordDef(Set<String> keywordDefsReferences) {
        JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
        builder.add("type", "object");
        JsonArrayBuilder arrayBuilder = jsonProvider.createArrayBuilder();
        for (String artefactImplReference : keywordDefsReferences) {
            arrayBuilder.add(addRef(jsonProvider.createObjectBuilder(), artefactImplReference));
        }
        builder.add("oneOf", arrayBuilder);
        return builder;
    }

    protected Map<String, JsonObjectBuilder> createKeywordImplDefs() throws JsonSchemaPreparationException {
        HashMap<String, JsonObjectBuilder> result = new HashMap<>();
        List<Class<? extends Function>> automationPackageKeywords = yamlKeywordsLookuper.getAutomationPackageKeywords();
        for (Class<? extends Function> automationPackageKeyword : automationPackageKeywords) {
            String yamlName = yamlKeywordsLookuper.getAutomationPackageKeywordName(automationPackageKeyword);
            String defName = yamlName + "Def";
            result.put(defName, createKeywordImplDef(yamlName, automationPackageKeyword));
        }
        return result;
    }

    private JsonObjectBuilder createKeywordImplDef(String yamlName, Class<? extends Function> keywordClass) throws JsonSchemaPreparationException {
        JsonObjectBuilder res = jsonProvider.createObjectBuilder();
        res.add("type", "object");

        // artefact has the top-level property matching the artefact name
        JsonObjectBuilder artefactNameProperty = jsonProvider.createObjectBuilder();

        // other properties are located in nested object and automatically prepared via reflection
        JsonObjectBuilder keywordProperties = jsonProvider.createObjectBuilder();
        fillKeywordProperties(keywordClass, keywordProperties, yamlName);

        // use camelCase for artefact names in yaml
        artefactNameProperty.add(yamlName, jsonProvider.createObjectBuilder().add("type", "object").add("properties", keywordProperties));
        res.add("properties", artefactNameProperty);
        res.add("additionalProperties", false);
        return res;
    }

    protected List<JsonSchemaDefinitionCreator> getDefinitionsExtensions() {
        List<JsonSchemaDefinitionCreator> extensions = new ArrayList<>();
        // TODO: add special annotation for extensions
        return extensions;
    }


    private void fillKeywordProperties(Class<? extends Function> keywordClass, JsonObjectBuilder keywordProperties, String yamlName) throws JsonSchemaPreparationException {
        this.schemaHelper.extractPropertiesFromClass(jsonSchemaCreator, keywordClass, keywordProperties, yamlName);
    }

    protected FieldMetadataExtractor prepareMetadataExtractor() {
        List<FieldMetadataExtractor> extractors = new ArrayList<>();

        extractors.add(new TokenSelectionCriteriaRule().getFieldMetadataExtractor());
        extractors.add(new DefaultFieldMetadataExtractor());

        return new AggregatingFieldMetadataExtractor(extractors);
    }

    protected List<JsonSchemaFieldProcessor> prepareFieldProcessors() {
        List<JsonSchemaFieldProcessor> result = new ArrayList<>();

        // -- BASIC PROCESSING RULES
        result.add(new CommonFilteredFieldProcessor());
        result.add(new TechnicalFieldRule().getJsonSchemaFieldProcessor(jsonProvider));
        result.add(new TokenSelectionCriteriaRule().getJsonSchemaFieldProcessor(jsonProvider));

        // -- RULES FROM EXTENSIONS HAVE LESS PRIORITY THAN BASIC RULES, BUT MORE PRIORITY THAN OTHER RULES
        result.addAll(getFieldExtensions());

        // -- RULES FOR OS KEYWORDS

        // -- SOME DEFAULT RULES FOR ENUMS AND DYNAMIC FIELDS
        result.add(new DynamicValueFieldProcessor(jsonProvider));
        result.add(new EnumFieldProcessor(jsonProvider));

        return result;
    }

    protected List<JsonSchemaFieldProcessor> getFieldExtensions() {
        return yamlKeywordsLookuper.getAllConversionRules()
                .stream()
                .map(r -> r.getJsonSchemaFieldProcessor(jsonProvider))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    public JsonObjectBuilder addRef(JsonObjectBuilder builder, String refValue){
        return builder.add("$ref", "#/$defs/" + refValue);
    }

}
