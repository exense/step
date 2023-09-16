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
package step.automation.packages.yaml.rules;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.json.spi.JsonProvider;
import step.automation.packages.yaml.deserialization.YamlKeywordFieldDeserializationProcessor;
import step.functions.Function;
import step.handlers.javahandler.jsonschema.FieldMetadata;
import step.handlers.javahandler.jsonschema.FieldMetadataExtractor;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;

public class TokenSelectionCriteriaRule implements YamlKeywordConversionRule {

    public static final String TOKEN_SELECTION_CRITERIA_YAML_FIELD = "routing";

    @Override
    public FieldMetadataExtractor getFieldMetadataExtractor() {
        return field -> {
            if (Function.class.equals(field.getDeclaringClass()) && field.getName().equals(Function.TOKEN_SELECTION_CRITERIA)) {
                return new FieldMetadata(TOKEN_SELECTION_CRITERIA_YAML_FIELD, null, Object.class, false);
            }
            return null;
        };
    }

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        return (aClass, field, fieldMetadata, jsonObjectBuilder, list) -> {
            if (Function.class.isAssignableFrom(aClass) && field.getName().equals(Function.TOKEN_SELECTION_CRITERIA)) {
                jsonObjectBuilder.add(TOKEN_SELECTION_CRITERIA_YAML_FIELD, jsonProvider.createObjectBuilder().add("type", "object"));
                return true;
            }
            return false;
        };
    }

    @Override
    public YamlKeywordFieldDeserializationProcessor getDeserializationProcessor() {
        return (keywordClass, field, output, codec) -> {
            if (field.getKey().equals(TOKEN_SELECTION_CRITERIA_YAML_FIELD)) {
                JsonNode selectionCriteriaValue = field.getValue();
                output.set(Function.TOKEN_SELECTION_CRITERIA, selectionCriteriaValue);
                return true;
            }
            return false;
        };
    }
}
