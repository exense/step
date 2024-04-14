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
import step.core.scanner.CachedAnnotationScanner;
import step.core.yaml.schema.AggregatedJsonSchemaFieldProcessor;
import step.core.yaml.schema.JsonSchemaExtension;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.handlers.javahandler.jsonschema.JsonSchemaCreator;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.jsonschema.DefaultFieldMetadataExtractor;

import java.util.*;

import static step.core.scanner.Classes.newInstanceAs;

public class YamlKeywordSchemaGenerator {

    public static final String KEYWORD_DEF = "KeywordDef";

    private final jakarta.json.spi.JsonProvider jsonProvider;

    protected final YamlJsonSchemaHelper schemaHelper;

    protected final AutomationPackageKeywordsLookuper automationPackageKeywordsLookuper = new AutomationPackageKeywordsLookuper();
    private final JsonSchemaCreator jsonSchemaCreator;

    public YamlKeywordSchemaGenerator(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
        this.schemaHelper = new YamlJsonSchemaHelper(jsonProvider);

        this.jsonSchemaCreator = new JsonSchemaCreator(
                jsonProvider,
                new AggregatedJsonSchemaFieldProcessor(YamlJsonSchemaHelper.prepareDefaultFieldProcessors(null)),
                new DefaultFieldMetadataExtractor()
        );
    }

    protected List<JsonSchemaDefinitionCreator> getDefinitionsExtensions() {
        List<JsonSchemaDefinitionCreator> extensions = new ArrayList<>();
        CachedAnnotationScanner.getClassesWithAnnotation(JsonSchemaDefinitionAddOn.LOCATION, JsonSchemaDefinitionAddOn.class, Thread.currentThread().getContextClassLoader()).stream()
                .map(newInstanceAs(JsonSchemaDefinitionCreator.class)).forEach(extensions::add);
        return extensions;
    }

    /**
     * Prepares definitions to be reused in json subschemas
     */
    public JsonObjectBuilder createKeywordDefs() throws JsonSchemaPreparationException {
        JsonObjectBuilder defsBuilder = jsonProvider.createObjectBuilder();

        List<JsonSchemaExtension> definitionCreators = new ArrayList<>();

        // prepare definitions for generic DynamicValue class
        definitionCreators.add((defsList, provider) -> {
            Map<String, JsonObjectBuilder> dynamicValueDefs = schemaHelper.createDynamicValueImplDefs();
            for (Map.Entry<String, JsonObjectBuilder> dynamicValueDef : dynamicValueDefs.entrySet()) {
                defsBuilder.add(dynamicValueDef.getKey(), dynamicValueDef.getValue());
            }
        });

        // prepare definitions for keyword classes
        definitionCreators.add((defsLis, provider) -> {
            Map<String, JsonObjectBuilder> keywordImplDefs = createKeywordImplDefs();
            for (Map.Entry<String, JsonObjectBuilder> artefactImplDef : keywordImplDefs.entrySet()) {
                defsBuilder.add(artefactImplDef.getKey(), artefactImplDef.getValue());
            }

            // add definition for "anyOf" artefact definitions prepared above
            defsBuilder.add(KEYWORD_DEF, createKeywordDef(keywordImplDefs.keySet()));
        });

        for (JsonSchemaExtension definitionCreator : definitionCreators) {
            definitionCreator.addToJsonSchema(defsBuilder, jsonProvider);
        }

        return defsBuilder;
    }

    protected JsonObjectBuilder createKeywordDef(Set<String> keywordDefsReferences) {
        JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
        builder.add("type", "object");
        JsonArrayBuilder arrayBuilder = jsonProvider.createArrayBuilder();
        for (String keywordImplReference : keywordDefsReferences) {
            JsonObjectBuilder builder1 = jsonProvider.createObjectBuilder();
            arrayBuilder.add(YamlJsonSchemaHelper.addRef(builder1, keywordImplReference));
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

}
