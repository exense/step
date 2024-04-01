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

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import step.automation.packages.AutomationPackageNamedEntityUtils;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.datapool.DataPoolConfiguration;
import step.handlers.javahandler.jsonschema.FieldMetadata;
import step.handlers.javahandler.jsonschema.JsonSchemaCreator;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.jsonschema.JsonSchema;

import java.lang.reflect.Field;
import java.util.List;

@JsonSchema(customJsonSchemaProcessor = AbstractYamlDataSource.YamlDataSourceSchemaProcessor.class)
public abstract class AbstractYamlDataSource<T extends DataPoolConfiguration> {

    public abstract T createDataPoolConfiguration();

    public abstract void fillDataPoolConfiguration(T config);

    public abstract void fillFromDataPoolConfiguration(T dataPoolConfiguration);

    public static class YamlDataSourceSchemaProcessor implements JsonSchemaFieldProcessor {
        @Override
        public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput, JsonSchemaCreator schemaCreator) {
            try {
                propertiesBuilder.add("type", "object");
                JsonArrayBuilder arrayBuilder = schemaCreator.getJsonProvider().createArrayBuilder();
                YamlJsonSchemaHelper schemaHelper = new YamlJsonSchemaHelper(schemaCreator.getJsonProvider());
                for (Class<? extends AbstractYamlDataSource<?>> dataSourceClass : YamlDataSourceLookuper.getYamlDataSources()) {
                    String entityName = AutomationPackageNamedEntityUtils.getEntityNameByClass(dataSourceClass);
                    arrayBuilder.add(schemaHelper.createNamedObjectImplDef(entityName, dataSourceClass, schemaCreator, false));
                }
                propertiesBuilder.add("oneOf", arrayBuilder);
                return true;
            } catch (JsonSchemaPreparationException e) {
                throw new RuntimeException("Unable to prepare json schema for datasource", e);
            }
        }
    }
}
