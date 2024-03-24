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
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.handlers.javahandler.jsonschema.FieldMetadata;
import step.handlers.javahandler.jsonschema.JsonSchema;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;

import java.lang.reflect.Field;
import java.util.List;

@JsonSchema(customJsonSchemaProcessor = YamlKeywordDefinition.YamlKeywordDefinitionJsonSchemaProcessor.class)
public class YamlKeywordDefinition {

    private String keywordName;
    private String keywordSelectionCriteriaJson;

    public YamlKeywordDefinition() {
    }

    public YamlKeywordDefinition(String keywordName, String keywordSelectionCriteria) {
        this.keywordName = keywordName;
        this.keywordSelectionCriteriaJson = keywordSelectionCriteria;
    }

    public String getKeywordName() {
        return keywordName;
    }

    public void setKeywordName(String keywordName) {
        this.keywordName = keywordName;
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
