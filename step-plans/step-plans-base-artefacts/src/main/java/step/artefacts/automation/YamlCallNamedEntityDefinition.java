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
package step.artefacts.automation;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.handlers.javahandler.jsonschema.FieldMetadata;
import step.handlers.javahandler.jsonschema.JsonSchemaCreator;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.jsonschema.JsonSchema;

import java.lang.reflect.Field;
import java.util.List;

@JsonSchema(customJsonSchemaProcessor = YamlCallNamedEntityDefinition.YamlCallNamedEntityDefinitionJsonSchemaProcessor.class)
public class YamlCallNamedEntityDefinition {

    private final String entityName;
    private String simpleEntityName;
    private String entitySelectionCriteriaJson;

    public YamlCallNamedEntityDefinition(String entityName, String simpleEntityName, String entitySelectionCriteria) {
        this.entityName = entityName;
        this.simpleEntityName = simpleEntityName;
        this.entitySelectionCriteriaJson = entitySelectionCriteria;
    }

    public DynamicValue<String> toDynamicValue() {
        if (entitySelectionCriteriaJson != null) {
            return new DynamicValue<>(entitySelectionCriteriaJson);
        } else {
            return new DynamicValue<>("{}");
        }
    }

    public static YamlCallNamedEntityDefinition fromDynamicValue(DynamicValue<String> dynamicValue) {
        try {
            if (dynamicValue.isDynamic()) {
                throw new UnsupportedOperationException("Dynamic arguments are not supported");
            }
            return new YamlCallNamedEntityDefinition(
                YamlCallNamedEntityDefinitionSerializer.getEntityName(dynamicValue, false),
                YamlCallNamedEntityDefinitionSerializer.getEntityName(dynamicValue, true),
                dynamicValue.getValue()
            );
        } catch (Exception ex) {
            throw new RuntimeException("Unable to parse keyword definition", ex);
        }
    }

    public String getEntityName() {
        return this.entityName;
    }

    public String getSimpleEntityName() {
        return simpleEntityName;
    }

    public void setSimpleEntityName(String simpleEntityName) {
        this.simpleEntityName = simpleEntityName;
    }

    public String getEntitySelectionCriteriaJson() {
        return entitySelectionCriteriaJson;
    }

    public void setEntitySelectionCriteriaJson(String entitySelectionCriteriaJson) {
        this.entitySelectionCriteriaJson = entitySelectionCriteriaJson;
    }

    public static class YamlCallNamedEntityDefinitionJsonSchemaProcessor implements JsonSchemaFieldProcessor {

        @Override
        public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput, JsonSchemaCreator schemaCreator) throws JsonSchemaPreparationException {
            JsonProvider jsonProvider = JsonProvider.provider();
            YamlJsonSchemaHelper jsonSchemaHelper = new YamlJsonSchemaHelper(jsonProvider);
            JsonArrayBuilder oneOfArrayBuilder = jsonProvider.createArrayBuilder();
            oneOfArrayBuilder
                .add(jsonProvider.createObjectBuilder().add("type", "string"))
                .add(jsonSchemaHelper.createPatternPropertiesWithDynamicValues());
            propertiesBuilder.add("oneOf", oneOfArrayBuilder);
            return true;
        }
    }
}
