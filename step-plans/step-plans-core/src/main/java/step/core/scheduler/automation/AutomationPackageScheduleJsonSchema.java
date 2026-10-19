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
package step.core.scheduler.automation;

import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.automation.packages.schema.CommonAutomationPackageJsonSchemaExtension;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.handlers.javahandler.jsonschema.FieldMetadata;
import step.handlers.javahandler.jsonschema.JsonSchemaCreator;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;

import java.lang.reflect.Field;
import java.util.List;

public class AutomationPackageScheduleJsonSchema extends CommonAutomationPackageJsonSchemaExtension {

    public AutomationPackageScheduleJsonSchema() {
        super(AutomationPackageSchedule.SCHEDULE_DEF, AutomationPackageSchedule.FIELD_NAME_IN_AP, AutomationPackageSchedule.class);
    }

    @Override
    protected List<JsonSchemaFieldProcessor> getFieldProcessors() {
        return YamlJsonSchemaHelper.prepareDefaultFieldProcessors(List.of(AutomationPackageScheduleJsonSchema::processCronExclusions));
    }

    /**
     * A cron exclusion is either a cron expression or an object holding the cron expression and its description
     */
    private static boolean processCronExclusions(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder,
                                                 List<String> requiredPropertiesOutput, JsonSchemaCreator schemaCreator) {
        if (!objectClass.equals(AutomationPackageSchedule.class) || !field.getName().equals("cronExclusions")) {
            return false;
        }
        JsonProvider jsonProvider = schemaCreator.getJsonProvider();
        JsonObjectBuilder stringType = jsonProvider.createObjectBuilder().add("type", "string");
        JsonObjectBuilder objectType = jsonProvider.createObjectBuilder()
            .add("type", "object")
            .add("properties", jsonProvider.createObjectBuilder()
                .add(AutomationPackageCronExclusion.CRON_FIELD, jsonProvider.createObjectBuilder().add("type", "string"))
                .add(AutomationPackageCronExclusion.DESCRIPTION_FIELD, jsonProvider.createObjectBuilder().add("type", "string")))
            .add("required", jsonProvider.createArrayBuilder().add(AutomationPackageCronExclusion.CRON_FIELD))
            .add("additionalProperties", false);
        propertiesBuilder.add(fieldMetadata.getFieldName(), jsonProvider.createObjectBuilder()
            .add("type", "array")
            .add("items", jsonProvider.createObjectBuilder()
                .add("oneOf", jsonProvider.createArrayBuilder().add(stringType).add(objectType))));
        return true;
    }
}
