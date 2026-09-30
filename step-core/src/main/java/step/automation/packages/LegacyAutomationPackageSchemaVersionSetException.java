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
        super(message("The automation package", declaredVersion, currentVersion));
    }

    /**
     * @param fragment the fragment declaring the older version, for instance its path relative to the package
     */
    public LegacyAutomationPackageSchemaVersionSetException(String fragment, String declaredVersion, String currentVersion) {
        super(message("The automation package fragment " + fragment, declaredVersion, currentVersion));
    }

    private static String message(String subject, String declaredVersion, String currentVersion) {
        return subject + " declares the schema version " + declaredVersion + ", older than the current one ("
            + currentVersion + "). The automation package has to be upgraded before it can be opened for editing: the "
            + "upgrade migrates to the current schema all the files of the package written against an older version, "
            + "the descriptor as well as the fragments. Comments in the rewritten files may be lost.";
    }
}
