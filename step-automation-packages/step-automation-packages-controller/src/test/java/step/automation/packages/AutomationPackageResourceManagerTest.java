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

package step.automation.packages;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.maven.MavenArtifactIdentifier;
import step.repositories.artifact.ResolvedMavenArtifact;
import step.repositories.artifact.SnapshotMetadata;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceMissingException;
import step.resources.ResourceRevisionFileHandle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static step.automation.packages.AutomationPackageUpdateStatus.CREATED;
import static step.automation.packages.RefreshResourceResult.ResultStatus.REFRESHED;

public class AutomationPackageResourceManagerTest extends AbstractAutomationPackageManagerTest {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageResourceManagerTest.class);

    @Test
    public void testAutomationPackageLibResourceCrud() throws AutomationPackageReadingException, AutomationPackageUnsupportedResourceTypeException, IOException {
        // *** PREPARE TEST DATA ***
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);
        File kwLibSnapshotJar = new File("src/test/resources/samples/" + KW_LIB_FILE_NAME);
        File kwLibUpdatedSnapshotJar = new File("src/test/resources/samples/" + KW_LIB_FILE_UPDATED_NAME);

        MavenArtifactIdentifier sampleSnapshot = new MavenArtifactIdentifier("test-group", "ap1", "1.0.0-SNAPSHOT", null, null);
        MavenArtifactIdentifier kwLibSnapshot = new MavenArtifactIdentifier("test-group", "test-kw-lib", "1.0.0-SNAPSHOT", null, null);

        MockedAutomationPackageProvidersResolver providersResolver = (MockedAutomationPackageProvidersResolver) manager.getProvidersResolver();
        Long firstTimestamp = System.currentTimeMillis();
        providersResolver.getMavenArtifactMocks().put(sampleSnapshot, new ResolvedMavenArtifact(automationPackageJar, new SnapshotMetadata("some timestamp", firstTimestamp, 1, true)));
        providersResolver.getMavenArtifactMocks().put(kwLibSnapshot,new ResolvedMavenArtifact( kwLibSnapshotJar, new SnapshotMetadata("some timestamp", firstTimestamp, 1, true)));

        AutomationPackageUpdateParameter apUpdateParams = new AutomationPackageUpdateParameterBuilder().forJunit().build();


        // *** START TEST ***

        // 1. UPLOAD LIB RESOURCE
        Resource uploadedResource = manager.getAutomationPackageResourceManager().uploadOrReuseAutomationPackageLibrary(
                manager.getAutomationPackageLibraryProvider(AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot), o -> true),
                null,
                apUpdateParams,
                false
        );
        ResourceRevisionFileHandle resourceFileHandleBeforeRefresh = resourceManager.getResourceFile(uploadedResource.getId().toHexString());
        log.info("Resource after the first upload: {}", resourceFileHandleBeforeRefresh.getResourceFile().getAbsolutePath());

        // check resource metadata
        checkResourceMetadata(uploadedResource, ResourceManager.RESOURCE_TYPE_AP_LIBRARY, firstTimestamp, kwLibSnapshot.toShortString());

        // check that stored content is valid
        Assert.assertArrayEquals(Files.readAllBytes(kwLibSnapshotJar.toPath()), Files.readAllBytes(resourceFileHandleBeforeRefresh.getResourceFile().toPath()));

        // 2. CREATE LINKED AUTOMATION PACKAGE
        AutomationPackageUpdateResult createdPackage = manager.createOrUpdateAutomationPackage(
                new AutomationPackageUpdateParameterBuilder().forJunit()
                        .withApSource(AutomationPackageFileSource.withMavenIdentifier(sampleSnapshot))
                        .withApLibrarySource(AutomationPackageFileSource.withResourceId(uploadedResource.getId().toString()))
                        .build()
        );
        Assert.assertEquals(CREATED, createdPackage.getStatus());
        AutomationPackage apBeforeRefresh = manager.getAutomationPackageById(createdPackage.getId(), o -> true);
        Long updatedTimestampBeforeRefresh = apBeforeRefresh.getLastModificationDate().toInstant().toEpochMilli();

        // 3. REFRESH THE LIB RESOURCE (UPLOAD NEW SNAPSHOT)
        Long secondTimestamp = System.currentTimeMillis();
        providersResolver.getMavenArtifactMocks().put(kwLibSnapshot, new ResolvedMavenArtifact(kwLibUpdatedSnapshotJar, new SnapshotMetadata("new timestamp", secondTimestamp, 2, true)));

        RefreshResourceResult refreshResourceResult = manager.getAutomationPackageResourceManager().refreshResourceAndLinkedPackages(uploadedResource.getId().toHexString(), apUpdateParams, manager);
        Assert.assertEquals(REFRESHED, refreshResourceResult.getResultStatus());
        Assert.assertTrue(refreshResourceResult.getErrorMessages().isEmpty());

        // actualize resource metadata after refresh
        uploadedResource = resourceManager.getResource(uploadedResource.getId().toHexString());

        // check resource metadata after refresh
        checkResourceMetadata(uploadedResource, ResourceManager.RESOURCE_TYPE_AP_LIBRARY, secondTimestamp, kwLibSnapshot.toShortString());

        // get resource file after refresh
        ResourceRevisionFileHandle resourceFileHandleAfterRefresh = resourceManager.getResourceFile(uploadedResource.getId().toHexString());
        log.info("Resource after refresh: {}", resourceFileHandleAfterRefresh.getResourceFile().getAbsolutePath());

        // check that stored content is valid
        Assert.assertArrayEquals(Files.readAllBytes(kwLibUpdatedSnapshotJar.toPath()), Files.readAllBytes(resourceFileHandleAfterRefresh.getResourceFile().toPath()));

        AutomationPackage apAfterRefresh = manager.getAutomationPackageById(createdPackage.getId(), o -> true);
        Long updatedTimestampAfterRefresh = apAfterRefresh.getLastModificationDate().toInstant().toEpochMilli();

        // update timestamp in AP has been actualized
        Assert.assertTrue(updatedTimestampAfterRefresh > updatedTimestampBeforeRefresh);

        // check old resource file is removed and not locked in file system
        // TODO: maybe we need to delete old resource revision file (first snapshot)
        // Assert.assertFalse(resourceFileHandleBeforeRefresh.getResourceFile().exists());

        // 4. DELETE RESOURCE IS NOT ALLOWED BECAUSE OF LINKED AUTOMATION PACKAGES
        try {
            manager.getAutomationPackageResourceManager().deleteResource(uploadedResource.getId().toHexString(), apUpdateParams.writeAccessValidator);
            Assert.fail("Automation package exception should be thrown");
        } catch (AutomationPackageAccessException ex){
            log.info("Caught exception: {}", ex.getMessage());
        }

        // 5. REMOVE AP, BUT DO NOT REMOVE THE LINKED RESOURCE (removeMainResourcesIfPossible=FALSE)
        manager.removeAutomationPackage(createdPackage.getId(), apUpdateParams.actorUser, apUpdateParams.objectPredicate, apUpdateParams.writeAccessValidator, false);

        // check that resource file is not yet removed
        Assert.assertNotNull(resourceManager.getResource(uploadedResource.getId().toHexString()));
        Assert.assertTrue(resourceFileHandleAfterRefresh.getResourceFile().exists());

        // 6. NOW RESOURCE CAN BE DELETED
        manager.getAutomationPackageResourceManager().deleteResource(uploadedResource.getId().toHexString(), apUpdateParams.writeAccessValidator);

        // check that resource is removed and not locked in file system
        try {
            resourceManager.getResource(uploadedResource.getId().toHexString());
        } catch (ResourceMissingException ex) {
            log.info("Resource has been successfully deleted: {}", uploadedResource.getResourceName());
        }
        Assert.assertFalse(resourceFileHandleBeforeRefresh.getResourceFile().exists());
        Assert.assertFalse(resourceFileHandleAfterRefresh.getResourceFile().exists());
    }

    private static void checkResourceMetadata(Resource resource, String expectedResourceType, Long expectedOriginTimestamp, String expectedOrigin) {
        Assert.assertEquals(expectedResourceType, resource.getResourceType());
        Assert.assertEquals(expectedOrigin, resource.getOrigin());
        Assert.assertEquals(expectedOriginTimestamp, resource.getOriginTimestamp());
    }
}