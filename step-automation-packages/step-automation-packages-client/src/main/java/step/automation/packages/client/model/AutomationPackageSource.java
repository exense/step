/*
 * ******************************************************************************
 *  * Copyright (C) 2020, exense GmbH
 *  *
 *  * This file is part of STEP
 *  *
 *  * STEP is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * STEP is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *  *****************************************************************************
 */
package step.automation.packages.client.model;

import java.io.File;

public class AutomationPackageSource {

    private String mavenSnippet;
    private File file;
    private String managedLibraryName;

    public static AutomationPackageSource withMavenSnippet(String mavenSnippet) {
        AutomationPackageSource automationPackageSource = new AutomationPackageSource();
        automationPackageSource.mavenSnippet = mavenSnippet;
        return automationPackageSource;
    }

    public static AutomationPackageSource withFile(File file) {
        AutomationPackageSource automationPackageSource = new AutomationPackageSource();
        automationPackageSource.file = file;
        return automationPackageSource;
    }

    public static AutomationPackageSource withManagedLibraryName(String managedLibraryName) {
        AutomationPackageSource automationPackageSource = new AutomationPackageSource();
        automationPackageSource.managedLibraryName = managedLibraryName;
        return automationPackageSource;
    }

    public String getMavenSnippet() {
        return mavenSnippet;
    }

    public File getFile() {
        return file;
    }

    public String getManagedLibraryName() {
        return managedLibraryName;
    }
}
