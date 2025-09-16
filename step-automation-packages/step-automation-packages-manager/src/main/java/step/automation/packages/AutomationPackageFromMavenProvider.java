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

import step.automation.packages.kwlibrary.AutomationPackageKeywordLibraryProvider;
import step.core.maven.MavenArtifactIdentifier;
import step.resources.ResourceOrigin;

import java.io.File;
import java.io.IOException;

public class AutomationPackageFromMavenProvider implements AutomationPackageArchiveProvider {

    protected final MavenArtifactIdentifier mavenArtifactIdentifier;
    private final AutomationPackageKeywordLibraryProvider keywordLibraryProvider;
    protected final AutomationPackageMavenConfig mavenConfig;

    public AutomationPackageFromMavenProvider(AutomationPackageMavenConfig mavenConfig,
                                              MavenArtifactIdentifier mavenArtifactIdentifier,
                                              AutomationPackageKeywordLibraryProvider keywordLibraryProvider) {
        this.mavenConfig = mavenConfig;
        this.mavenArtifactIdentifier = mavenArtifactIdentifier;
        this.keywordLibraryProvider = keywordLibraryProvider;
    }

    @Override
    public AutomationPackageArchive getAutomationPackageArchive() throws AutomationPackageReadingException {
        // The same client as in MavenArtifactRepository
        return new AutomationPackageArchive(getFile(), keywordLibraryProvider == null ? null : keywordLibraryProvider.getKeywordLibrary());
    }

    protected File getFile() throws AutomationPackageReadingException {
        return MavenArtifactDownloader.getFile(mavenConfig, mavenArtifactIdentifier);
    }

    @Override
    public ResourceOrigin getOrigin() {
        return mavenArtifactIdentifier;
    }

    @Override
    public void close() throws IOException {

    }
}
