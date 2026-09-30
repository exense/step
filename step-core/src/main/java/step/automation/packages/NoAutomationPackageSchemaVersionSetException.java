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

public class NoAutomationPackageSchemaVersionSetException extends AutomationPackageUpgradeRequiredException {
    public NoAutomationPackageSchemaVersionSetException(String currentVersion) {
        super("The automation package declares no schema version. It has to be upgraded before it can be opened for "
            + "editing: the upgrade considers it as written against the current schema version (" + currentVersion
            + ") and declares that version, without migrating its files. If the package was written against an older "
            + "version, declare that version in its descriptor instead, so that the upgrade migrates its files.");
    }

}
