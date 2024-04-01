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
import step.automation.packages.AutomationPackageNamedEntityUtils;
import step.automation.packages.model.AbstractYamlFunction;
import step.automation.packages.yaml.AutomationPackageKeywordsLookuper;
import step.core.yaml.schema.*;
import step.handlers.javahandler.jsonschema.*;
import step.jsonschema.DefaultFieldMetadataExtractor;

import java.util.*;

public class YamlKeywordSchemaGenerator {

    public static final String KEYWORD_DEF = "KeywordDef";

    private final jakarta.json.spi.JsonProvider jsonProvider;

    protected final YamlJsonSchemaHelper schemaHelper;

    protected final AutomationPackageKeywordsLookuper automationPackageKeywordsLookuper = new AutomationPackageKeywordsLookuper();
    private final JsonSchemaCreator jsonSchemaCreator;

    public YamlKeywordSchemaGenerator(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
        this.schemaHelper = new YamlJsonSchemaHelper(jsonProvider);

        // --- Fields metadata rules (fields we want to rename)
        FieldMetadataExtractor fieldMetadataExtractor = new DefaultFieldMetadataExtractor();

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

        // prepare definitions for keyword classes
        definitionCreators.add((defsList) -> {
            Map<String, JsonObjectBuilder> keywordImplDefs = createKeywordImplDefs();
            for (Map.Entry<String, JsonObjectBuilder> artefactImplDef : keywordImplDefs.entrySet()) {
                defsBuilder.add(artefactImplDef.getKey(), artefactImplDef.getValue());
            }

            // add definition for "anyOf" artefact definitions prepared above
            defsBuilder.add(KEYWORD_DEF, createKeywordDef(keywordImplDefs.keySet()));
        });

        for (JsonSchemaDefinitionCreator definitionCreator : definitionCreators) {
            definitionCreator.addDefinition(defsBuilder);
        }

        return defsBuilder;
    }

    protected JsonObjectBuilder createKeywordDef(Set<String> keywordDefsReferences) {
        JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
        builder.add("type", "object");
        JsonArrayBuilder arrayBuilder = jsonProvider.createArrayBuilder();
        for (String keywordImplReference : keywordDefsReferences) {
            arrayBuilder.add(addRef(jsonProvider.createObjectBuilder(), keywordImplReference));
        }
        builder.add("oneOf", arrayBuilder);
        return builder;
    }

    protected Map<String, JsonObjectBuilder> createKeywordImplDefs() throws JsonSchemaPreparationException {
        HashMap<String, JsonObjectBuilder> result = new HashMap<>();
        List<Class<? extends AbstractYamlFunction<?>>> automationPackageKeywords = automationPackageKeywordsLookuper.getAutomationPackageKeywords();
        for (Class<? extends AbstractYamlFunction<?>> automationPackageKeyword : automationPackageKeywords) {
            String yamlName = AutomationPackageNamedEntityUtils.getEntityNameByClass(automationPackageKeyword);
            String defName = yamlName + "Def";
            result.put(defName, schemaHelper.createNamedObjectImplDef(yamlName, automationPackageKeyword, jsonSchemaCreator, false));
        }
        return result;
    }

    protected List<JsonSchemaFieldProcessor> prepareFieldProcessors() {
        List<JsonSchemaFieldProcessor> result = new ArrayList<>();

        // -- BASIC PROCESSING RULES
        result.add(new CommonFilteredFieldProcessor());
        result.add(new DynamicValueFieldProcessor());
        result.add(new EnumFieldProcessor());

        return result;
    }

    public JsonObjectBuilder addRef(JsonObjectBuilder builder, String refValue){
        return builder.add("$ref", "#/$defs/" + refValue);
    }

}
