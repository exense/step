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
package step.core;

public interface Constants {
    String STEP_API_VERSION_STRING = "3.28.0";
    Version STEP_API_VERSION = new Version(STEP_API_VERSION_STRING);

    String STEP_YAML_SCHEMA_VERSION_STRING = "1.1.1";
    /**
     * This version is used for both the Yaml plan schema version and the automation package schema version (YamlPlanVersions.class, YamlAutomationPackageVersions.class)
     * It is used by extension for EE too (YamlReaderExtenderEE.class, YamlAutomationPackageVersionsEE.class).
     * <br/>
     * Whenever one of these schema is changed we must bump the version (but only once per Step release) and update the related <a href="https://step.dev/knowledgebase/devops/automation-package-yaml/#schema-version">documentation</a>
     * When bumping the version here, we must also update the version in the generated JSON schema files accordingly (Junit will detect the mismatch otherwise ). You will find the 4 files paths in the above-mentioned classes
     */
    Version STEP_YAML_SCHEMA_VERSION = new Version(STEP_YAML_SCHEMA_VERSION_STRING);
}
