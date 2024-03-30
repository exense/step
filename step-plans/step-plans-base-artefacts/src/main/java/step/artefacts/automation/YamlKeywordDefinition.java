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
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.handlers.javahandler.jsonschema.FieldMetadata;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.jsonschema.JsonSchema;

import java.lang.reflect.Field;
import java.util.List;

@JsonSchema(customJsonSchemaProcessor = YamlKeywordDefinition.YamlKeywordDefinitionJsonSchemaProcessor.class)
public class YamlKeywordDefinition {

    private String keywordName;
    private String simpleKeywordName;
    private String keywordSelectionCriteriaJson;

    public YamlKeywordDefinition(String keywordName, String simpleKeywordName, String keywordSelectionCriteria) {
        this.keywordName = keywordName;
        this.simpleKeywordName = simpleKeywordName;
        this.keywordSelectionCriteriaJson = keywordSelectionCriteria;
    }

    public DynamicValue<String> toDynamicValue() {
        if (keywordSelectionCriteriaJson != null) {
            return new DynamicValue<>(keywordSelectionCriteriaJson);
        } else {
            return new DynamicValue<>("{}");
        }
    }

    public static YamlKeywordDefinition fromDynamicValue(DynamicValue<String> dynamicValue) {
        try {
            if (dynamicValue.isDynamic()) {
                throw new UnsupportedOperationException("Dynamic arguments are not supported");
            }
            return new YamlKeywordDefinition(
                    YamlKeywordDefinitionSerializer.getFunctionName(dynamicValue, DefaultJacksonMapperProvider.getObjectMapper(), false),
                    YamlKeywordDefinitionSerializer.getFunctionName(dynamicValue, DefaultJacksonMapperProvider.getObjectMapper(), true),
                    dynamicValue.getValue()
            );
        } catch (Exception ex) {
            throw new RuntimeException("Unable to parse keyword definition", ex);
        }
    }

    public String getKeywordName(){
       return this.keywordName;
    }

    public String getSimpleKeywordName() {
        return simpleKeywordName;
    }

    public void setSimpleKeywordName(String simpleKeywordName) {
        this.simpleKeywordName = simpleKeywordName;
    }

    public String getKeywordSelectionCriteriaJson() {
        return keywordSelectionCriteriaJson;
    }

    public void setKeywordSelectionCriteriaJson(String keywordSelectionCriteriaJson) {
        this.keywordSelectionCriteriaJson = keywordSelectionCriteriaJson;
    }

    public static class YamlKeywordDefinitionJsonSchemaProcessor implements JsonSchemaFieldProcessor {

        @Override
        public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput) throws JsonSchemaPreparationException {
            JsonProvider jsonProvider = JsonProvider.provider();
            YamlJsonSchemaHelper jsonSchemaHelper = new YamlJsonSchemaHelper(jsonProvider);
            JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();
            JsonArrayBuilder oneOfArrayBuilder = jsonProvider.createArrayBuilder();
            oneOfArrayBuilder
                    .add(jsonProvider.createObjectBuilder().add("type", "string"))
                    .add(jsonSchemaHelper.createPatternPropertiesWithDynamicValues());
            nestedPropertyParamsBuilder.add("oneOf", oneOfArrayBuilder);
            propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
            return true;
        }
    }
}
