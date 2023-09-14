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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.yaml.YamlKeywordsLookuper;
import step.automation.packages.yaml.rules.TechnicalFieldRule;
import step.core.Version;
import step.core.yaml.schema.*;
import step.functions.Function;
import step.handlers.javahandler.jsonschema.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class YamlAutomationPackageSchemaGenerator {
    private static final Logger log = LoggerFactory.getLogger(YamlAutomationPackageSchemaGenerator.class);

    private static final String KEYWORD_DEF = "KeywordDef";

    protected final String targetPackage;

    protected final Version actualVersion;

    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected final JsonProvider jsonProvider = JsonProvider.provider();

    protected final JsonSchemaCreator jsonSchemaCreator;
    protected final YamlJsonSchemaHelper dynamicValuesHelper = new YamlJsonSchemaHelper(jsonProvider);
    protected final YamlKeywordsLookuper yamlKeywordsLookuper = new YamlKeywordsLookuper();

    public YamlAutomationPackageSchemaGenerator(String targetPackage, Version actualVersion) {
        this.targetPackage = targetPackage;
        this.actualVersion = actualVersion;

      // --- Fields metadata rules (fields we want to rename)
        FieldMetadataExtractor fieldMetadataExtractor = prepareMetadataExtractor();

        List<JsonSchemaFieldProcessor> processingRules = prepareFieldProcessors();
        this.jsonSchemaCreator = new JsonSchemaCreator(jsonProvider, new AggregatedJsonSchemaFieldProcessor(processingRules), fieldMetadataExtractor);
    }

    protected FieldMetadataExtractor prepareMetadataExtractor() {
        return new DefaultFieldMetadataExtractor();
    }

    protected List<JsonSchemaFieldProcessor> prepareFieldProcessors() {
        List<JsonSchemaFieldProcessor> result = new ArrayList<>();

        // -- BASIC PROCESSING RULES
        result.add(new CommonFilteredFieldProcessor());
        result.add(new TechnicalFieldRule().getJsonSchemaFieldProcessor(jsonProvider));

        // -- RULES FROM EXTENSIONS HAVE LESS PRIORITY THAN BASIC RULES, BUT MORE PRIORITY THAN OTHER RULES
        // TODO: token selection format???
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

    protected List<JsonSchemaDefinitionCreator> getDefinitionsExtensions() {
        List<JsonSchemaDefinitionCreator> extensions = new ArrayList<>();
        // TODO: add special annotation for extensions
        return extensions;
    }

    public JsonNode generateJsonSchema() throws JsonSchemaPreparationException {
        JsonObjectBuilder topLevelBuilder = jsonProvider.createObjectBuilder();

        // common fields for json schema
        topLevelBuilder.add("$schema", "http://json-schema.org/draft-07/schema#");
        topLevelBuilder.add("title", "Step Keyword Package");
        topLevelBuilder.add("type", "object");

        // prepare definitions to be reused in subschemas (referenced via $ref property)
        topLevelBuilder.add("$defs", createDefs());

        // add properties for top-level
        topLevelBuilder.add("properties", createPackageProperties());
        topLevelBuilder.add("required", jsonProvider.createArrayBuilder());
        topLevelBuilder.add( "additionalProperties", false);

        // convert jakarta objects to jackson JsonNode
        try {
            return fromJakartaToJsonNode(topLevelBuilder);
        } catch (JsonProcessingException e) {
            throw new JsonSchemaPreparationException("Unable to convert json to jackson jsonNode", e);
        }
    }

    private JsonObjectBuilder createPackageProperties() {
        JsonObjectBuilder objectBuilder = jsonProvider.createObjectBuilder();
        // in 'version' we should either explicitly specify the current json schema version or skip this field
        objectBuilder.add("version", jsonProvider.createObjectBuilder().add("const", actualVersion.toString()));
        objectBuilder.add("keywords",
                jsonProvider.createObjectBuilder().add("type", "array").add("items", addRef(jsonProvider.createObjectBuilder(), KEYWORD_DEF)));
        return objectBuilder;
    }

    public static JsonObjectBuilder addRef(JsonObjectBuilder builder, String refValue){
        return builder.add("$ref", "#/$defs/" + refValue);
    }

    private JsonNode fromJakartaToJsonNode(JsonObjectBuilder objectBuilder) throws JsonProcessingException {
        return objectMapper.readTree(objectBuilder.build().toString());
    }

    /**
     * Prepares definitions to be reused in json subschemas
     */
    private JsonObjectBuilder createDefs() throws JsonSchemaPreparationException {
        JsonObjectBuilder defsBuilder = jsonProvider.createObjectBuilder();

        List<JsonSchemaDefinitionCreator> definitionCreators = new ArrayList<>();

        // prepare definitions for generic DynamicValue class
        definitionCreators.add((defsList) -> {
            Map<String, JsonObjectBuilder> dynamicValueDefs = dynamicValuesHelper.createDynamicValueImplDefs();
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

    private void fillKeywordProperties(Class<? extends Function> keywordClass, JsonObjectBuilder keywordProperties, String yamlName) throws JsonSchemaPreparationException {
        log.info("Preparing json schema for keyword class {}...", keywordClass);

        // analyze hierarchy of class annotated with @Artefact
        List<Field> allFieldsInHierarchy = new ArrayList<>();
        Class<?> currentClass = keywordClass;
        while (currentClass != null) {
            allFieldsInHierarchy.addAll(List.of(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }
        Collections.reverse(allFieldsInHierarchy);

        // for each field we want either build the json schema via reflection
        // or use some predefined schemas for some special classes (like step.core.dynamicbeans.DynamicValue)
        try {
            jsonSchemaCreator.processFields(keywordClass, keywordProperties, allFieldsInHierarchy, new ArrayList<>());
        } catch (Exception ex) {
            throw new JsonSchemaPreparationException("Unable to process keyword " + yamlName, ex);
        }
    }

}
