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
import step.automation.packages.model.AutomationPackageAlertingRule;
import step.automation.packages.yaml.rules.YamlConversionRule;
import step.automation.packages.yaml.rules.YamlConversionRuleAddOn;
import step.core.scanner.CachedAnnotationScanner;
import step.core.yaml.schema.*;
import step.handlers.javahandler.jsonschema.DefaultFieldMetadataExtractor;
import step.handlers.javahandler.jsonschema.JsonSchemaCreator;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.plugins.alerting.event.AlertingEventInterface;
import step.plugins.alerting.rule.condition.AlertingRuleAction;
import step.plugins.alerting.rule.condition.AlertingRuleCondition;

import java.util.*;
import java.util.stream.Collectors;

import static step.core.scanner.Classes.newInstanceAs;

public class YamlAlertingRuleSchemaGenerator {

    public static final String ALERTING_RULE_DEF = "AlertingRuleDef";
    public static final String ALERTING_RULE_CONDITION_DEF = "AlertingRuleConditionDef";
    public static final String ALERTING_RULE_ACTION_DEF = "AlertingRuleActionDef";

    private final jakarta.json.spi.JsonProvider jsonProvider;

    protected final JsonSchemaCreator jsonSchemaCreator;

    protected final YamlJsonSchemaHelper schemaHelper;

    public YamlAlertingRuleSchemaGenerator(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
        this.schemaHelper = new YamlJsonSchemaHelper(this.jsonProvider);
        List<JsonSchemaFieldProcessor> fieldProcessors = prepareFieldProcessors();
        this.jsonSchemaCreator = new JsonSchemaCreator(jsonProvider, new AggregatedJsonSchemaFieldProcessor(fieldProcessors), new DefaultFieldMetadataExtractor());
    }

    /**
     * Prepares definitions to be reused in json subschemas
     */
    public JsonObjectBuilder createAlertingRulesDefs() throws JsonSchemaPreparationException {
        JsonObjectBuilder defsBuilder = jsonProvider.createObjectBuilder();

        List<JsonSchemaDefinitionCreator> definitionCreators = new ArrayList<>();

        // prepare definitions for generic DynamicValue class
        definitionCreators.add((defsList) -> {
            Map<String, JsonObjectBuilder> dynamicValueDefs = schemaHelper.createDynamicValueImplDefs();
            for (Map.Entry<String, JsonObjectBuilder> dynamicValueDef : dynamicValueDefs.entrySet()) {
                defsBuilder.add(dynamicValueDef.getKey(), dynamicValueDef.getValue());
            }
        });

        // prepare definitions for AutomationPackageAlertingRule content
        definitionCreators.add(defsList -> {
            // scan annotated Conditions to find all supported implementations
            Map<String, JsonObjectBuilder> conditionsDef = createDefsForAnnotatedNamedEntities(AlertingRuleCondition.class);
            for (Map.Entry<String, JsonObjectBuilder> conditionImplDef : conditionsDef.entrySet()) {
                defsBuilder.add(conditionImplDef.getKey(), conditionImplDef.getValue());
            }

            defsBuilder.add(ALERTING_RULE_CONDITION_DEF, createOneOfDef(conditionsDef.keySet().stream().sorted().collect(Collectors.toList())));
        });

        definitionCreators.add(defsList -> {
            // scan annotated Actions to find all supported implementations
            Map<String, JsonObjectBuilder> actionsDef = createDefsForAnnotatedNamedEntities(AlertingRuleAction.class);
            for (Map.Entry<String, JsonObjectBuilder> actionImplDef : actionsDef.entrySet()) {
                defsBuilder.add(actionImplDef.getKey(), actionImplDef.getValue());
            }

            defsBuilder.add(ALERTING_RULE_ACTION_DEF, createOneOfDef(actionsDef.keySet().stream().sorted().collect(Collectors.toList())));
        });

        // prepare the main definition for AutomationPackageAlertingRule class
        definitionCreators.add(defsList -> {
            JsonObjectBuilder res = jsonProvider.createObjectBuilder();
            res.add("type", "object");

            JsonObjectBuilder properties = jsonProvider.createObjectBuilder();
            schemaHelper.extractPropertiesFromClass(jsonSchemaCreator, AutomationPackageAlertingRule.class, properties, "alertingRule");

            res.add("properties", properties);
            res.add("additionalProperties", false);

            defsBuilder.add(ALERTING_RULE_DEF, res);
        });

        for (JsonSchemaDefinitionCreator definitionCreator : definitionCreators) {
            definitionCreator.addDefinition(defsBuilder);
        }

        return defsBuilder;
    }

    protected JsonObjectBuilder createOneOfDef(List<String> references) {
        JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
        builder.add("type", "object");
        JsonArrayBuilder arrayBuilder = jsonProvider.createArrayBuilder();
        for (String conditionDefReference : references) {
            arrayBuilder.add(addRef(jsonProvider.createObjectBuilder(), conditionDefReference));
        }
        builder.add("oneOf", arrayBuilder);
        return builder;
    }

    private Map<String, JsonObjectBuilder> createDefsForAnnotatedNamedEntities(Class<?> entityClass) throws JsonSchemaPreparationException {
        HashMap<String, JsonObjectBuilder> result = new HashMap<>();

        List<? extends Class<?>> conditionClasses = AutomationPackageNamedEntityUtils.scanNamedEntityClasses(entityClass);
        for (Class<?> conditionClass : conditionClasses) {
            String name = conditionClass.getSimpleName();
            String defName = name + "Def";
            result.put(defName, schemaHelper.createNamedObjectImplDef(name, conditionClass, jsonSchemaCreator));
        }
        return result;
    }

    protected List<JsonSchemaFieldProcessor> prepareFieldProcessors() {
        List<JsonSchemaFieldProcessor> result = new ArrayList<>();

        // -- BASIC PROCESSING RULES
        result.add(new CommonFilteredFieldProcessor());

        // event classes
        result.add((objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            if (objectClass.equals(AutomationPackageAlertingRule.class) && field.getName().equals(AutomationPackageAlertingRule.EVENT_CLASS_FIELD)) {
                JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();
                JsonArrayBuilder arrayBuilder = jsonProvider.createArrayBuilder();

                List<Class<?>> annotatedAlertingEvents = AutomationPackageNamedEntityUtils.scanNamedEntityClasses(AlertingEventInterface.class);
                for (Class<?> annotatedAlertingEvent : annotatedAlertingEvents) {
                    arrayBuilder.add(annotatedAlertingEvent.getSimpleName());
                }

                nestedPropertyParamsBuilder.add("enum", arrayBuilder);
                propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
                return true;
            }
            return false;
        });

        // conditions
        result.add((objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            if (objectClass.equals(AutomationPackageAlertingRule.class) && field.getName().equals(AutomationPackageAlertingRule.CONDITIONS_FIELD)) {
                JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();
                nestedPropertyParamsBuilder.add("type", "array");
                nestedPropertyParamsBuilder.add("items", addRef(jsonProvider.createObjectBuilder(), ALERTING_RULE_CONDITION_DEF));
                propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
                return true;
            }
            return false;
        });

        // -- RULES FROM EXTENSIONS HAVE LESS PRIORITY THAN BASIC RULES, BUT MORE PRIORITY THAN OTHER RULES
        result.addAll(getConditionFieldExtensions());

        // -- SOME DEFAULT RULES FOR ENUMS AND DYNAMIC FIELDS
        result.add(new DynamicValueFieldProcessor(jsonProvider));
        result.add(new EnumFieldProcessor(jsonProvider));

        return result;
    }

    protected List<JsonSchemaFieldProcessor> getConditionFieldExtensions() {
        return CachedAnnotationScanner.getClassesWithAnnotation(YamlConversionRuleAddOn.LOCATION, YamlConversionRuleAddOn.class, Thread.currentThread().getContextClassLoader()).stream()
                .filter(c -> {
                    Class<?>[] classes = c.getAnnotation(YamlConversionRuleAddOn.class).targetClasses();
                    return classes == null || classes.length == 0 || Arrays.stream(classes).anyMatch(AlertingRuleCondition.class::isAssignableFrom);
                })
                .filter(YamlConversionRule.class::isAssignableFrom)
                .map(newInstanceAs(YamlConversionRule.class))
                .map(r -> r.getJsonSchemaFieldProcessor(jsonProvider))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public JsonObjectBuilder addRef(JsonObjectBuilder builder, String refValue) {
        return builder.add("$ref", "#/$defs/" + refValue);
    }
}
