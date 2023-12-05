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
package step.engine.plugins;

import ch.exense.commons.app.Configuration;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.plugins.Plugin;

@Plugin()
public class AutomationPackageJsonSchemaPlugin extends AbstractExecutionEnginePlugin {
    public static final String PROP_AUTOMATION_PACKAGE_JSON_SCHEMA = "plugins.automation.jsonschema";

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
        if (context.getOperationMode() == OperationMode.LOCAL) {
            Configuration config = context.getConfiguration();
            config.putProperty(PROP_AUTOMATION_PACKAGE_JSON_SCHEMA, YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH);
        }
    }
}
