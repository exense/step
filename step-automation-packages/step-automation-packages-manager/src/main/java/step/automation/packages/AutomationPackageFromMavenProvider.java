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
import step.repositories.artifact.MavenArtifactClient;

import java.io.File;
import java.io.IOException;

public class AutomationPackageFromMavenProvider implements AutomationPackageArchiveProvider {

    private final String mavenSettingsXml;
    private final File localRepository;
    private final String artifactId;
    private final String groupId;
    private final String version;
    private final String classifier;

    public AutomationPackageFromMavenProvider(String mavenSettingsXml,
                                              File localRepository,
                                              String artifactId,
                                              String groupId,
                                              String version,
                                              String classifier) {
        this.mavenSettingsXml = mavenSettingsXml;
        this.localRepository = localRepository;
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.version = version;
        this.classifier = classifier;
    }

    @Override
    public AutomationPackageArchive getAutomationPackageArchive() throws AutomationPackageReadingException {
        // The same client as in MavenArtifactRepository
        try {
            MavenArtifactClient mavenArtifactClient = new MavenArtifactClient(mavenSettingsXml, localRepository);
            File artifact = mavenArtifactClient.getArtifact(new DefaultArtifact(groupId, artifactId, classifier, "jar", version));
            return new AutomationPackageArchive(artifact);
        } catch (SettingsBuildingException | org.eclipse.aether.resolution.ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {

    }
}
