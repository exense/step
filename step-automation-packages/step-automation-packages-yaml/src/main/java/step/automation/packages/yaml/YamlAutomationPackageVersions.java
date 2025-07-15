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
package step.automation.packages.yaml;

import step.core.Version;

import static step.core.Constants.STEP_YAML_SCHEMA_VERSION;

public class YamlAutomationPackageVersions {

    /**
     * While technically the Yaml plan and the Automation Package schema versions could be different, we decided to align them and always keep them the same for the sake of simplicity and maintainability
     * For any changes to the schema, bump the version {@link step.core.Constants#STEP_YAML_SCHEMA_VERSION} and follow the details documented on that field
     */
    public static final Version ACTUAL_VERSION = STEP_YAML_SCHEMA_VERSION;
    public static final String ACTUAL_JSON_SCHEMA_PATH = "step/automation/packages/yaml/step-automation-package-schema-os-" + ACTUAL_VERSION + ".json";
}
