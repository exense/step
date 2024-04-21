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
package step.parameter.automation;

import jakarta.json.JsonObjectBuilder;
import step.automation.packages.model.AutomationPackageSchedule;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.automation.packages.yaml.schema.AutomationPackageJsonSchemaExtension;
import step.core.yaml.schema.AggregatedJsonSchemaFieldProcessor;
import step.core.yaml.schema.JsonSchemaDefinitionExtension;
import step.core.yaml.schema.JsonSchemaExtension;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.handlers.javahandler.jsonschema.JsonSchemaCreator;
import step.jsonschema.DefaultFieldMetadataExtractor;

import java.util.ArrayList;
import java.util.List;

public class AutomationPackageParameterJsonSchemaExtension implements AutomationPackageJsonSchemaExtension {

    @Override
    public List<JsonSchemaDefinitionExtension> getExtendedDefinitions() {
        return List.of((jsonSchemaBuilder, jsonProvider) -> {
            YamlJsonSchemaHelper schemaHelper = new YamlJsonSchemaHelper(jsonProvider);
            JsonSchemaCreator jsonSchemaCreator = new JsonSchemaCreator(jsonProvider, new AggregatedJsonSchemaFieldProcessor(new ArrayList<>()), new DefaultFieldMetadataExtractor());
            JsonObjectBuilder res = schemaHelper.createJsonSchemaForClass(jsonSchemaCreator, AutomationPackageParameter.class, false);
            jsonSchemaBuilder.add(AutomationPackageParameter.DEF_NAME_IN_JSON_SCHEMA, res);
        });
    }

    @Override
    public List<JsonSchemaExtension> getAdditionalAutomationPackageFields() {
        return List.of((jsonSchemaBuilder, jsonProvider) -> jsonSchemaBuilder.add(AutomationPackageParameter.DEF_NAME_IN_JSON_SCHEMA,
                jsonProvider.createObjectBuilder()
                        .add("type", "array")
                        .add("items", YamlJsonSchemaHelper.addRef(jsonProvider.createObjectBuilder(), AutomationPackageParameter.DEF_NAME_IN_JSON_SCHEMA))
        ));
    }
}
