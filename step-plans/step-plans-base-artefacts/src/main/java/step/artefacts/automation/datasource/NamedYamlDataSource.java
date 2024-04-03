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
package step.artefacts.automation.datasource;

import jakarta.json.*;
import step.automation.packages.AutomationPackageNamedEntityUtils;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.handlers.javahandler.jsonschema.FieldMetadata;
import step.handlers.javahandler.jsonschema.JsonSchemaCreator;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.jsonschema.JsonSchema;

import java.lang.reflect.Field;
import java.util.List;

@JsonSchema(customJsonSchemaProcessor = NamedYamlDataSource.SchemaProcessor.class)
public class NamedYamlDataSource {

    private AbstractYamlDataSource<?> yamlDataSource;

    public NamedYamlDataSource(AbstractYamlDataSource<?> yamlDataSource) {
        this.yamlDataSource = yamlDataSource;
    }

    public AbstractYamlDataSource<?> getYamlDataSource() {
        return yamlDataSource;
    }

    public void setYamlDataSource(AbstractYamlDataSource<?> yamlDataSource) {
        this.yamlDataSource = yamlDataSource;
    }

    public static class SchemaProcessor implements JsonSchemaFieldProcessor {
        @Override
        public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput, JsonSchemaCreator schemaCreator) {
            try {
                propertiesBuilder.add("type", "object");
                JsonArrayBuilder arrayBuilder = schemaCreator.getJsonProvider().createArrayBuilder();
                YamlJsonSchemaHelper schemaHelper = new YamlJsonSchemaHelper(schemaCreator.getJsonProvider());
                for (Class<? extends AbstractYamlDataSource<?>> yamlDataSourceClass : YamlDataSourceLookuper.getYamlDataSources()) {
                    String entityName = AutomationPackageNamedEntityUtils.getEntityNameByClass(YamlDataSourceLookuper.resolveDataPool(yamlDataSourceClass));
                    JsonObjectBuilder dataPoolBuilder = schemaHelper.createNamedObjectImplDef(entityName, yamlDataSourceClass, schemaCreator, false);
                    if (!isForWriteEditable()) {
                        // remove "forWrite" field
                        JsonObject namedEntityObject = dataPoolBuilder.build();
                        JsonObject properties1 = (JsonObject) namedEntityObject.get("properties");
                        JsonObject propertiesContainer = (JsonObject) properties1.get(entityName);

                        JsonObject properties2 = (JsonObject) propertiesContainer.get("properties");

                        dataPoolBuilder.addAll(schemaCreator.getJsonProvider().createObjectBuilder(namedEntityObject))
                                .add("properties", schemaCreator.getJsonProvider().createObjectBuilder(properties1)
                                        .add(entityName, schemaCreator.getJsonProvider().createObjectBuilder(propertiesContainer)
                                                .add("properties", schemaCreator.getJsonProvider().createObjectBuilder(properties2)
                                                        .remove(AbstractYamlDataSource.FOR_WRITE_FIELD))
                                        )
                                );
                    }
                    arrayBuilder.add(dataPoolBuilder.build());
                }
                propertiesBuilder.add("oneOf", arrayBuilder);
                return true;
            } catch (JsonSchemaPreparationException e) {
                throw new RuntimeException("Unable to prepare json schema for datasource", e);
            }
        }

        protected boolean isForWriteEditable(){
            return false;
        }
    }

    public static class WithForWriteSchemaProcessor extends SchemaProcessor {

        @Override
        protected boolean isForWriteEditable(){
            return true;
        }
    }
}
