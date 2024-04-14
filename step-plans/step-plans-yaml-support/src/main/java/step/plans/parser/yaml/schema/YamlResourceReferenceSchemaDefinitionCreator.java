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
package step.plans.parser.yaml.schema;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.core.yaml.schema.JsonSchemaDefinitionAddOn;
import step.core.yaml.schema.JsonSchemaExtension;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;

import java.util.HashMap;
import java.util.Map;

@JsonSchemaDefinitionAddOn
public class YamlResourceReferenceSchemaDefinitionCreator implements JsonSchemaExtension {

    public static final String RESOURCE_REFERENCE_DEF = "resourceReference";
    public static final String FILE_REFERENCE_RESOURCE_ID_FIELD = "id";

    @Override
    public void addToJsonSchema(JsonObjectBuilder jsonSchemaBuilder, JsonProvider jsonProvider) throws JsonSchemaPreparationException {
        Map<String, JsonObjectBuilder> dynamicValueDefs = createResourceReferenceDefs(jsonProvider);
        for (Map.Entry<String, JsonObjectBuilder> dynamicValueDef : dynamicValueDefs.entrySet()) {
            jsonSchemaBuilder.add(dynamicValueDef.getKey(), dynamicValueDef.getValue());
        }
    }

    public Map<String, JsonObjectBuilder> createResourceReferenceDefs(JsonProvider jsonProvider) {
        Map<String, JsonObjectBuilder> res = new HashMap<>();
        JsonArrayBuilder oneOf = jsonProvider.createArrayBuilder();
        oneOf.add(jsonProvider.createObjectBuilder().add("type", "string"));
        oneOf.add(jsonProvider.createObjectBuilder()
                .add("type", "object")
                .add("additionalProperties", false)
                .add("properties", jsonProvider.createObjectBuilder()
                        .add(FILE_REFERENCE_RESOURCE_ID_FIELD, jsonProvider.createObjectBuilder()
                                .add("type", "string")))
        );
        res.put(RESOURCE_REFERENCE_DEF, jsonProvider.createObjectBuilder().add("oneOf", oneOf));
        return res;
    }

}
