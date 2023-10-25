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
package step.automation.packages.yaml.schema;

import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.automation.packages.yaml.model.AutomationPackageSchedulerTask;
import step.core.yaml.schema.AggregatedJsonSchemaFieldProcessor;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.handlers.javahandler.jsonschema.DefaultFieldMetadataExtractor;
import step.handlers.javahandler.jsonschema.JsonSchemaCreator;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;

import java.util.ArrayList;

public class YamlSchedulerSchemaGenerator {

    public static final String SCHEDULER_TASK_DEF = "SchedulerTaskDef";

    private final jakarta.json.spi.JsonProvider jsonProvider;

    protected final JsonSchemaCreator jsonSchemaCreator;

    protected final YamlJsonSchemaHelper schemaHelper;

    public YamlSchedulerSchemaGenerator(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
        this.schemaHelper = new YamlJsonSchemaHelper(this.jsonProvider);
        this.jsonSchemaCreator = new JsonSchemaCreator(jsonProvider, new AggregatedJsonSchemaFieldProcessor(new ArrayList<>()), new DefaultFieldMetadataExtractor());
    }

    /**
     * Prepares definitions to be reused in json subschemas
     */
    public JsonObjectBuilder createSchedulerTaskDefs() throws JsonSchemaPreparationException {
        JsonObjectBuilder defsBuilder = jsonProvider.createObjectBuilder();

        JsonObjectBuilder res = jsonProvider.createObjectBuilder();
        res.add("type", "object");

        JsonObjectBuilder schedulerTaskProperties = jsonProvider.createObjectBuilder();
        schemaHelper.extractPropertiesFromClass(this.jsonSchemaCreator, AutomationPackageSchedulerTask.class, schedulerTaskProperties, "scheduler");

        res.add("properties", schedulerTaskProperties);
        res.add("additionalProperties", false);

        defsBuilder.add(SCHEDULER_TASK_DEF, res);

        return defsBuilder;
    }

    public JsonObjectBuilder addRef(JsonObjectBuilder builder, String refValue){
        return builder.add("$ref", "#/$defs/" + refValue);
    }

}
