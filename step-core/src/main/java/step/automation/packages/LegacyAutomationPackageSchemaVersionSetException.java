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
package step.automation.packages;

public class LegacyAutomationPackageSchemaVersionSetException extends AutomationPackageUpgradeRequiredException {
    public LegacyAutomationPackageSchemaVersionSetException(String declaredVersion, String currentVersion) {
        super("The automation package declares the schema version " + declaredVersion + ", older than the current one ("
            + currentVersion + "). It has to be upgraded before it can be opened for editing, which migrates its files to "
            + "the current schema. Comments in the rewritten files may be lost.");
    }

}
