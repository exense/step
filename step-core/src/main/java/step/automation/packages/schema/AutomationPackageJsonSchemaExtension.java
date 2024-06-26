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
package step.automation.packages.schema;

import step.core.yaml.schema.JsonSchemaDefinitionExtension;
import step.core.yaml.schema.JsonSchemaExtension;

import java.util.List;

public interface AutomationPackageJsonSchemaExtension {

    /**
     * Provides the extensions to be used to fill the "defs" (definitions section) in json schema, i.e. to prepare
     * some sub-schemas to be reused in the main schema.
     */
    List<JsonSchemaDefinitionExtension> getExtendedDefinitions();

    /**
     * Provides the extensions to be used to add the new fields to the json schema of automation package, i.e. to add
     * some custom fields to the automation package.
     */
    List<JsonSchemaExtension> getAdditionalAutomationPackageFields();
}
