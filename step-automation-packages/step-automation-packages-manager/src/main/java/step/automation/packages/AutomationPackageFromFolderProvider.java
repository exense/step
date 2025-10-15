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

import step.automation.packages.library.AutomationPackageLibraryProvider;
import step.resources.ResourceOrigin;
import step.resources.UploadedResourceOrigin;

import java.io.File;
import java.io.IOException;

public class AutomationPackageFromFolderProvider implements AutomationPackageArchiveProvider {

    private final File apFolder;
    private final AutomationPackageLibraryProvider keywordLibraryProvider;

    public AutomationPackageFromFolderProvider(File apFolder, AutomationPackageLibraryProvider keywordLibraryProvider) {
        this.apFolder = apFolder;
        this.keywordLibraryProvider = keywordLibraryProvider;
    }

    @Override
    public AutomationPackageArchive getAutomationPackageArchive() throws AutomationPackageReadingException {
        return new JavaAutomationPackageArchive(apFolder, keywordLibraryProvider == null ? null : keywordLibraryProvider.getAutomationPackageLibrary());
    }

    @Override
    public ResourceOrigin getOrigin() {
        return new UploadedResourceOrigin();
    }

    @Override
    public void close() throws IOException {

    }
}
