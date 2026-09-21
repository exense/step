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
import jakarta.json.spi.JsonProvider;
import step.core.yaml.YamlMetadata;

/**
 * Prepares the definitions for the free-form metadata (see {@link YamlMetadata}). Any yaml content is accepted
 * within a map, but the keys are validated at any depth.
 * <pre>{@code
 * "MetadataDef": {
 *   "type": "object",
 *   "patternProperties": { "<key pattern>": { "$ref": "#/$defs/MetadataValueDef" } },
 *   "additionalProperties": false
 * },
 * "MetadataValueDef": {
 *   "type": [ "object", "array", "string", "number", "boolean", "null" ],
 *   "patternProperties": { "<key pattern>": { "$ref": "#/$defs/MetadataValueDef" } },
 *   "additionalProperties": false,
 *   "items": { "$ref": "#/$defs/MetadataValueDef" }
 * }
 * }</pre>
 * The keys are validated with "patternProperties" and "additionalProperties" rather than with the "propertyNames"
 * of draft 6, because the validator used to read the yaml files (everit 1.5.1) only supports draft 4.
 * For the same reason the accepted types of a value are listed explicitly: this old validator only applies the
 * keywords of a definition when its type says which of them apply.
 */
@JsonSchemaDefinitionAddOn
public class MetadataSchemaDefinitionCreator implements JsonSchemaExtension {

    @Override
    public void addToJsonSchema(JsonObjectBuilder defsBuilder, JsonProvider jsonProvider) {
        defsBuilder.add(YamlMetadata.METADATA_DEF, jsonProvider.createObjectBuilder()
            .add("type", "object")
            .add("description", "Free-form metadata")
            .add("patternProperties", createKeyDef(jsonProvider))
            .add("additionalProperties", false));

        defsBuilder.add(YamlMetadata.METADATA_VALUE_DEF, jsonProvider.createObjectBuilder()
            .add("type", jsonProvider.createArrayBuilder()
                .add("object").add("array").add("string").add("number").add("boolean").add("null"))
            .add("patternProperties", createKeyDef(jsonProvider))
            .add("additionalProperties", false)
            .add("items", YamlJsonSchemaHelper.addRef(jsonProvider.createObjectBuilder(), YamlMetadata.METADATA_VALUE_DEF)));
    }

    private static JsonObjectBuilder createKeyDef(JsonProvider jsonProvider) {
        return jsonProvider.createObjectBuilder()
            .add(YamlMetadata.KEY_PATTERN, YamlJsonSchemaHelper.addRef(jsonProvider.createObjectBuilder(), YamlMetadata.METADATA_VALUE_DEF));
    }
}
