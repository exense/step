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
package step.core.yaml.schema;

import jakarta.json.JsonObjectBuilder;
import step.core.yaml.YamlMetadata;
import step.handlers.javahandler.jsonschema.FieldMetadata;
import step.handlers.javahandler.jsonschema.JsonSchemaCreator;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * References the metadata definition (see {@link MetadataSchemaDefinitionCreator}) for the {@link YamlMetadata#METADATA_FIELD}
 * of the yaml models
 */
public class MetadataFieldProcessor implements JsonSchemaFieldProcessor {

    @Override
    public boolean applyCustomProcessing(Class<?> aClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder jsonObjectBuilder, List<String> list, JsonSchemaCreator schemaCreator) {
        if (field.getName().equals(YamlMetadata.METADATA_FIELD) && Map.class.isAssignableFrom(field.getType())) {
            jsonObjectBuilder.add(fieldMetadata.getFieldName(), YamlJsonSchemaHelper.addRef(schemaCreator.getJsonProvider().createObjectBuilder(), YamlMetadata.METADATA_DEF));
            return true;
        }
        return false;
    }
}
