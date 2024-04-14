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
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;

import java.util.Map;

@JsonSchemaDefinitionAddOn
public class DynamicValueSchemaDefinitionCreator implements JsonSchemaExtension {

    @Override
    public void addToJsonSchema(JsonObjectBuilder defsList, JsonProvider jsonProvider) throws JsonSchemaPreparationException {
        Map<String, JsonObjectBuilder> dynamicValueDefs = new YamlJsonSchemaHelper(jsonProvider).createDynamicValueImplDefs();
        for (Map.Entry<String, JsonObjectBuilder> dynamicValueDef : dynamicValueDefs.entrySet()) {
            defsList.add(dynamicValueDef.getKey(), dynamicValueDef.getValue());
        }
    }
}
