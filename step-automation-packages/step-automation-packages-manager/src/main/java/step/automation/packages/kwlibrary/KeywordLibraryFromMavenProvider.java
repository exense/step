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
package step.automation.packages.kwlibrary;

import step.automation.packages.AutomationPackageMavenConfig;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.MavenArtifactDownloader;
import step.core.maven.MavenArtifactIdentifier;
import step.resources.ResourceManager;
import step.resources.ResourceOrigin;

import java.io.File;
import java.io.IOException;

public class KeywordLibraryFromMavenProvider implements AutomationPackageKeywordLibraryProvider {

    private final AutomationPackageMavenConfig mavenConfig;
    private final MavenArtifactIdentifier mavenArtifactIdentifier;

    public KeywordLibraryFromMavenProvider(AutomationPackageMavenConfig mavenConfig,
                                           MavenArtifactIdentifier mavenArtifactIdentifier) {
        this.mavenConfig = mavenConfig;
        this.mavenArtifactIdentifier = mavenArtifactIdentifier;
    }

    @Override
    public File getKeywordLibrary() throws AutomationPackageReadingException {
        return MavenArtifactDownloader.getFile(mavenConfig, mavenArtifactIdentifier);
    }

    @Override
    public ResourceOrigin getOrigin() {
        return mavenArtifactIdentifier;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public String getTrackingValue() {
        return mavenArtifactIdentifier == null ? null : mavenArtifactIdentifier.toShortString();
    }

    @Override
    public String getResourceType() {
        return ResourceManager.RESOURCE_TYPE_MAVEN_ARTIFACT;
    }
}
