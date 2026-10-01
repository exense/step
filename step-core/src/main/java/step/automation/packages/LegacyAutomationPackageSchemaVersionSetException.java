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
    /**
     * @param outdatedFiles the number of files of the package written against an older version, descriptor and
     *                      fragments, all of them being migrated by the upgrade
     */
    public LegacyAutomationPackageSchemaVersionSetException(String declaredVersion, String currentVersion, int outdatedFiles) {
        super(message("The automation package", declaredVersion, currentVersion, outdatedFiles));
    }

    /**
     * @param fragment      the fragment declaring the older version, for instance its path relative to the package
     * @param outdatedFiles the number of files of the package written against an older version, descriptor and
     *                      fragments, all of them being migrated by the upgrade
     */
    public LegacyAutomationPackageSchemaVersionSetException(String fragment, String declaredVersion, String currentVersion, int outdatedFiles) {
        super(message("The automation package fragment " + fragment, declaredVersion, currentVersion, outdatedFiles));
    }

    private static String message(String subject, String declaredVersion, String currentVersion, int outdatedFiles) {
        return subject + " declares the schema version " + declaredVersion + ", older than the current one ("
            + currentVersion + ")."
            + (outdatedFiles > 1 ? " In total, " + outdatedFiles + " files of the package, descriptor and fragments, are "
            + "written against an older version." : "")
            + " The automation package has to be upgraded before it can be opened for editing, which migrates all its "
            + "outdated files to the current schema. Comments in the rewritten files may be lost.";
    }
}
