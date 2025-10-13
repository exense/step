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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.maven.MavenArtifactIdentifier;
import step.core.objectenricher.ObjectPredicate;
import step.repositories.artifact.ResolvedMavenArtifact;
import step.repositories.artifact.SnapshotMetadata;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceOrigin;

import java.io.IOException;
import java.util.List;

public class AbstractAutomationPackageFromMavenProvider implements AutomationPackageProvider {

    private static final Logger logger = LoggerFactory.getLogger(AbstractAutomationPackageFromMavenProvider.class);
    protected final MavenArtifactIdentifier mavenArtifactIdentifier;
    protected final AutomationPackageMavenConfig mavenConfig;
    protected final ResourceManager resourceManager;
    protected final ObjectPredicate objectPredicate;
    protected final ResolvedMavenArtifact resolvedMavenArtefact;

    public AbstractAutomationPackageFromMavenProvider(AutomationPackageMavenConfig mavenConfig,
                                                      MavenArtifactIdentifier mavenArtifactIdentifier,
                                                      ResourceManager resourceManager, ObjectPredicate objectPredicate) throws AutomationPackageReadingException {
        this.mavenConfig = mavenConfig;
        this.mavenArtifactIdentifier = mavenArtifactIdentifier;
        this.resourceManager = resourceManager;
        this.objectPredicate = objectPredicate;
        this.resolvedMavenArtefact = getResolvedMavenArtefact();
    }

    @Override
    public boolean isModifiableResource() {
        //Only newer snapshot will be modified, snapshotMetadata can be null in case it was not possible to extract them in which case we always update
        return getOrigin() != null && getOrigin().isModifiable() && (resolvedMavenArtefact == null || resolvedMavenArtefact.snapshotMetadata == null || resolvedMavenArtefact.snapshotMetadata.newSnapshotVersion);
    }

    protected ResolvedMavenArtifact getResolvedMavenArtefact() throws AutomationPackageReadingException {
        List<Resource> existingResource = lookupExistingResources(resourceManager, objectPredicate);

        if (existingResource != null && !existingResource.isEmpty()) {
            Resource resource = existingResource.get(0);

            // If an artefact resource with the same origin is found. If it is a snapshot with a newer version available, we fetch it
            // Otherwise we reuse the same resource
            if (isModifiableResource()) {
                SnapshotMetadata snapshotMetadata = MavenArtifactDownloader.fetchSnapshotMetadata(mavenConfig, mavenArtifactIdentifier, resource.getOriginTimestamp());
                if (snapshotMetadata.newSnapshotVersion) {
                    logger.debug("New snapshot version found for {}, downloading it", mavenArtifactIdentifier.toStringRepresentation());
                    return MavenArtifactDownloader.getFile(mavenConfig, mavenArtifactIdentifier, resource.getOriginTimestamp());
                } else {
                    //reuse resource
                    logger.debug("Latest snapshot version already downloaded for {}, reusing it", mavenArtifactIdentifier.toStringRepresentation());
                    return new ResolvedMavenArtifact(resourceManager.getResourceFile(resource.getId().toHexString()).getResourceFile(), null);
                }
            } else {
                //reuse resource
                logger.debug("Release maven artefact already downloaded for {}, reusing it", mavenArtifactIdentifier.toStringRepresentation());
                return new ResolvedMavenArtifact(resourceManager.getResourceFile(resource.getId().toHexString()).getResourceFile(), null);
            }
        } else {
            //this is a new resource we fetch it
            logger.debug("New maven artefact requested for {}, downloading it", mavenArtifactIdentifier.toStringRepresentation());
            return MavenArtifactDownloader.getFile(mavenConfig, mavenArtifactIdentifier, null);
        }
    }

    @Override
    public Long getSnapshotTimestamp() {
        return (resolvedMavenArtefact.snapshotMetadata) != null ? resolvedMavenArtefact.snapshotMetadata.timestamp: null;
    }

    @Override
    public boolean hasNewContent() {
        return (resolvedMavenArtefact.snapshotMetadata) != null && resolvedMavenArtefact.snapshotMetadata.newSnapshotVersion;
    }

    @Override
    public ResourceOrigin getOrigin() {
        return mavenArtifactIdentifier;
    }

    @Override
    public void close() throws IOException {

    }
}
