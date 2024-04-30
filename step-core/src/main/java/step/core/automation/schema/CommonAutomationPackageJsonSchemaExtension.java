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
package step.core.automation.schema;

import jakarta.json.JsonObjectBuilder;
import step.core.yaml.schema.AggregatedJsonSchemaFieldProcessor;
import step.core.yaml.schema.JsonSchemaDefinitionExtension;
import step.core.yaml.schema.JsonSchemaExtension;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.handlers.javahandler.jsonschema.JsonSchemaCreator;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.jsonschema.DefaultFieldMetadataExtractor;

import java.util.List;

public abstract class CommonAutomationPackageJsonSchemaExtension implements AutomationPackageJsonSchemaExtension {

    protected String fieldName;
    protected String defName;
    protected Class<?> objectClass;

    public CommonAutomationPackageJsonSchemaExtension(String defName, String fieldName, Class<?> objectClass) {
        this.defName = defName;
        this.fieldName = fieldName;
        this.objectClass = objectClass;
    }

    @Override
    public List<JsonSchemaDefinitionExtension> getExtendedDefinitions() {
        return List.of((jsonSchemaBuilder, jsonProvider) -> {
            YamlJsonSchemaHelper schemaHelper = new YamlJsonSchemaHelper(jsonProvider);
            JsonSchemaCreator jsonSchemaCreator = new JsonSchemaCreator(jsonProvider, new AggregatedJsonSchemaFieldProcessor(getFieldProcessors()), new DefaultFieldMetadataExtractor());
            JsonObjectBuilder res = schemaHelper.createJsonSchemaForClass(jsonSchemaCreator, objectClass, false);
            jsonSchemaBuilder.add(defName, res);
        });
    }

    protected List<JsonSchemaFieldProcessor> getFieldProcessors() {
        return YamlJsonSchemaHelper.prepareDefaultFieldProcessors(null);
    }

    @Override
    public List<JsonSchemaExtension> getAdditionalAutomationPackageFields() {
        return List.of((jsonSchemaBuilder, jsonProvider) -> jsonSchemaBuilder.add(fieldName,
                jsonProvider.createObjectBuilder()
                        .add("type", "array")
                        .add("items", YamlJsonSchemaHelper.addRef(jsonProvider.createObjectBuilder(), defName))
        ));
    }

}
