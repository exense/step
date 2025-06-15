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

import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.artifact.DefaultArtifact;
import step.core.maven.MavenArtifactIdentifier;
import step.repositories.artifact.MavenArtifactClient;

import java.io.File;
import java.io.IOException;

public class AutomationPackageFromMavenProvider implements AutomationPackageArchiveProvider {

    private final MavenArtifactIdentifier mavenArtifactIdentifier;
    private final AutomationPackageMavenConfig mavenConfig;

    public AutomationPackageFromMavenProvider(AutomationPackageMavenConfig mavenConfig,
                                              MavenArtifactIdentifier mavenArtifactIdentifier) {
        this.mavenConfig = mavenConfig;
        this.mavenArtifactIdentifier = mavenArtifactIdentifier;
    }

    @Override
    public AutomationPackageArchive getAutomationPackageArchive() throws AutomationPackageReadingException {
        // The same client as in MavenArtifactRepository
        try {
            if (mavenConfig == null) {
                throw new AutomationPackageReadingException("Maven config is not resolved");
            }
            if (mavenConfig.getMavenSettingsXml() == null) {
                throw new AutomationPackageManagerException("Maven settings xml is not resolved");
            }
            if (mavenConfig.getLocalFileRepository() == null) {
                throw new AutomationPackageManagerException("Maven local file repository is not resolved");
            }
            MavenArtifactClient mavenArtifactClient = new MavenArtifactClient(mavenConfig.getMavenSettingsXml(), mavenConfig.getLocalFileRepository());
            File artifact = mavenArtifactClient.getArtifact(new DefaultArtifact(
                    mavenArtifactIdentifier.getGroupId(),
                    mavenArtifactIdentifier.getArtifactId(),
                    mavenArtifactIdentifier.getClassifier(),
                    mavenArtifactIdentifier.getType() == null || mavenArtifactIdentifier.getType().isEmpty() ? "jar" : mavenArtifactIdentifier.getType(),
                    mavenArtifactIdentifier.getVersion())
            );
            return new AutomationPackageArchive(artifact);
        } catch (SettingsBuildingException | org.eclipse.aether.resolution.ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {

    }
}
