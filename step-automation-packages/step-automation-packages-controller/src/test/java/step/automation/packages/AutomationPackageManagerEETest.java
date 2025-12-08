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
import step.attachments.FileResolver;
import step.core.AbstractContext;
import step.core.maven.MavenArtifactIdentifier;
import step.core.objectenricher.*;
import step.repositories.artifact.ResolvedMavenArtifact;
import step.repositories.artifact.SnapshotMetadata;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceMissingException;
import step.resources.ResourceRevisionFileHandle;

import javax.xml.transform.Result;
import java.io.*;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

import static step.automation.packages.AutomationPackageUpdateStatus.CREATED;
import static step.automation.packages.AutomationPackageUpdateStatus.UPDATED;

public class AutomationPackageManagerEETest extends AbstractAutomationPackageManagerTest {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageManagerEETest.class);

    private static final String ATTRIBUTE_PROJECT_NAME = "project";
    private static final String GLOBAL_PROJECT = "globalProject";
    private static final String PROJECT_1 = "project1";
    private static final String PROJECT_2 = "project2";

    @Test
    public void testCrudWithPermissions() {
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);
        File extendedAutomationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_EXTENDED_FILE_NAME);
        File anotherAutomationPackageJar = new File("src/test/resources/samples/" + SAMPLE_ECHO_FILE_NAME);

        File libJar = new File("src/test/resources/samples/" + KW_LIB_FILE_NAME);
        File libJarUpdated = new File("src/test/resources/samples/" + KW_LIB_FILE_UPDATED_NAME);

        MavenArtifactIdentifier libVersion1 = new MavenArtifactIdentifier("test-group", "test-lib", "1.0.0-SNAPSHOT", null, null);
        MockedAutomationPackageProvidersResolver providersResolver = (MockedAutomationPackageProvidersResolver) manager.getProvidersResolver();
        providersResolver.getMavenArtifactMocks().put(libVersion1, new ResolvedMavenArtifact(libJar, new SnapshotMetadata("some timestamp", System.currentTimeMillis(), 1, false)));

        try (InputStream is = new FileInputStream(automationPackageJar);
             InputStream isExt = new FileInputStream(extendedAutomationPackageJar);
             InputStream isAnother = new FileInputStream(anotherAutomationPackageJar);
        ) {
            // 1. Save new automation package for tenant1
            AutomationPackageFileSource sample1ApSource = AutomationPackageFileSource.withInputStream(is, SAMPLE1_FILE_NAME);
            AutomationPackageFileSource extendedApSource = AutomationPackageFileSource.withInputStream(isExt, SAMPLE1_EXTENDED_FILE_NAME);
            AutomationPackageFileSource anotherApSource = AutomationPackageFileSource.withInputStream(isAnother, SAMPLE1_EXTENDED_FILE_NAME);
            AutomationPackageFileSource libSource = AutomationPackageFileSource.withMavenIdentifier(libVersion1);

            AutomationPackageUpdateParameter createParametersGlobal = new AutomationPackageUpdateParameterBuilder().forJunit()
                    .withApSource(sample1ApSource)
                    .withApLibrarySource(libSource)
                    .withAllowUpdate(false)
                    .withAsync(false)
                    .withCheckForSameOrigin(true)
                    .withEnricher(createTenantEnricher(GLOBAL_PROJECT))
                    .withWriteAccessValidator(createWriteAccessValidator(GLOBAL_PROJECT))
                    .withObjectPredicate(createAccessPredicate(GLOBAL_PROJECT, PROJECT_1))
                    .build();
            AutomationPackageUpdateResult resultGlobal = manager.createOrUpdateAutomationPackage(createParametersGlobal);
            Assert.assertEquals(CREATED, resultGlobal.getStatus());

            // all common checks for CRUD operations are covered in step.automation.packages.AutomationPackageManagerOSTest
            // here we only check restriction via tenants (enricher and object predicate)
            AutomationPackage apGlobal = automationPackageAccessor.get(resultGlobal.getId());

            // check if enricher fills the project attribute
            Assert.assertEquals(GLOBAL_PROJECT, apGlobal.getAttribute(ATTRIBUTE_PROJECT_NAME));

            // check if the project is also propagated to the resources
            Resource apResourceGlobal = resourceManager.getResource(FileResolver.resolveResourceId(apGlobal.getAutomationPackageResource()));
            Resource libResourceGlobal = resourceManager.getResource(FileResolver.resolveResourceId(apGlobal.getAutomationPackageLibraryResource()));

            Assert.assertEquals(GLOBAL_PROJECT, apResourceGlobal.getAttribute(ATTRIBUTE_PROJECT_NAME));
            Assert.assertEquals(GLOBAL_PROJECT, libResourceGlobal.getAttribute(ATTRIBUTE_PROJECT_NAME));

            // upload another ap in another project, but with the same (unmodified lib)
            AutomationPackageUpdateParameter createParameters1 = new AutomationPackageUpdateParameterBuilder().forJunit()
                    .withApSource(anotherApSource)
                    .withApLibrarySource(libSource)
                    .withAllowUpdate(false)
                    .withCheckForSameOrigin(true)
                    .withAsync(false)
                    .withEnricher(createTenantEnricher(PROJECT_1))
                    .withWriteAccessValidator(createWriteAccessValidator(PROJECT_1))
                    .withObjectPredicate(createAccessPredicate(GLOBAL_PROJECT, PROJECT_1))
                    .build();

            AutomationPackageUpdateResult result1 = manager.createOrUpdateAutomationPackage(createParameters1);
            Assert.assertEquals(CREATED, result1.getStatus());

            // check that ap library is reused
            AutomationPackage ap1 = automationPackageAccessor.get(result1.getId());
            Resource libResource1 = resourceManager.getResource(FileResolver.resolveResourceId(ap1.getAutomationPackageLibraryResource()));
            Assert.assertEquals(libResource1.getId(), libResourceGlobal.getId());

            // lib resource is still linked with global project
            Assert.assertEquals(GLOBAL_PROJECT, libResource1.getAttribute(ATTRIBUTE_PROJECT_NAME));

            // try to reload AP library (newSnapshotVersion=true) without permissions on project1
            providersResolver.getMavenArtifactMocks().put(libVersion1, new ResolvedMavenArtifact(
                    libJarUpdated,
                    new SnapshotMetadata("some timestamp", System.currentTimeMillis(), 1, true))
            );

            AutomationPackageUpdateParameter updateParameter =  new AutomationPackageUpdateParameterBuilder().forJunit()
                    .withApSource(extendedApSource)
                    .withApLibrarySource(libSource)
                    .withAllowUpdate(true)
                    .withForceRefreshOfSnapshots(true)
                    .withAsync(false)
                    .withCheckForSameOrigin(true)
                    .withEnricher(createTenantEnricher(GLOBAL_PROJECT))
                    .withWriteAccessValidator(createWriteAccessValidator(GLOBAL_PROJECT))
                    .withObjectPredicate(createAccessPredicate(GLOBAL_PROJECT, PROJECT_1))
                    .build();

            AutomationPackageUpdateResult result3 = manager.createOrUpdateAutomationPackage(updateParameter);
            Assert.assertEquals(UPDATED, result3.getStatus());

            // Try to delete global AP without permissions - it is restricted
            try {
                manager.removeAutomationPackage(apGlobal.getId(), "userProject1",
                        createAccessPredicate(GLOBAL_PROJECT),
                        createWriteAccessValidator(PROJECT_1)
                );
                Assert.fail("Exception should be thrown");
            } catch (AutomationPackageAccessException ex){
                log.info("Exception caught: {}", ex.getMessage());
            }

            // Delete global AP with enough permissions
            manager.removeAutomationPackage(apGlobal.getId(), "globalAdmin",
                    createAccessPredicate(GLOBAL_PROJECT),
                    createWriteAccessValidator(GLOBAL_PROJECT)
            );

            // check AP and resource is deleted, but AP lib still exists and used in Project1
            AutomationPackage automationPackage = automationPackageAccessor.get(apGlobal.getId());
            Assert.assertNull(automationPackage);

            try {
                resourceManager.getResource(apResourceGlobal.getId().toHexString());
                Assert.fail("Resource is not removed");
            } catch (ResourceMissingException ex){
                log.info("Resource is removed: {}", ex.getMessage());
            }

            Assert.assertNotNull(resourceManager.getResource(libResourceGlobal.getId().toHexString()));
            Assert.assertTrue(resourceManager.getResourceFile(libResourceGlobal.getId().toHexString()).getResourceFile().exists());

        } catch (IOException e) {
            throw new RuntimeException("IO Exception", e);
        }
    }

    @Test
    public void testManagedLibrary(){
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);
        File anotherAutomationPackageJar = new File("src/test/resources/samples/" + SAMPLE_ECHO_FILE_NAME);

        File libJar = new File("src/test/resources/samples/" + KW_LIB_FILE_NAME);
        File libJarUpdated = new File("src/test/resources/samples/" + KW_LIB_FILE_UPDATED_NAME);

        MavenArtifactIdentifier libVersion1 = new MavenArtifactIdentifier("test-group", "test-lib", "1.0.0-SNAPSHOT", null, null);
        MockedAutomationPackageProvidersResolver providersResolver = (MockedAutomationPackageProvidersResolver) manager.getProvidersResolver();
        providersResolver.getMavenArtifactMocks().put(libVersion1, new ResolvedMavenArtifact(libJar, new SnapshotMetadata("some timestamp", System.currentTimeMillis(), 1, false)));

        try (InputStream is = new FileInputStream(automationPackageJar)) {
            AutomationPackageFileSource sample1ApSource = AutomationPackageFileSource.withInputStream(is, SAMPLE1_FILE_NAME);
            AutomationPackageFileSource libSource = AutomationPackageFileSource.withMavenIdentifier(libVersion1);

            // 1. Create managed library by Global Admin
            AutomationPackageUpdateParameter globalAdminParams = new AutomationPackageUpdateParameterBuilder()
                    .forJunit()
                    .withActorUser("globalAdmin")
                    .withEnricher(createTenantEnricher(GLOBAL_PROJECT))
                    .withObjectPredicate(createAccessPredicate(GLOBAL_PROJECT))
                    .withWriteAccessValidator(createWriteAccessValidator(GLOBAL_PROJECT))
                    .build();

            Resource globalLibResource = manager.createAutomationPackageResource(ResourceManager.RESOURCE_TYPE_AP_MANAGED_LIBRARY, libSource, "testManagedLibrary", globalAdminParams);
            Assert.assertNotNull(globalLibResource);
            Assert.assertEquals(GLOBAL_PROJECT, globalLibResource.getAttribute(ATTRIBUTE_PROJECT_NAME));

            // 2. User in Project1 uses the managed library
            AutomationPackageUpdateParameter user1CreateApParams = new AutomationPackageUpdateParameterBuilder()
                    .forJunit()
                    .withApSource(sample1ApSource)
                    .withApLibrarySource(AutomationPackageFileSource.withResourceId(globalLibResource.getId().toHexString()))
                    .withAllowUpdate(false)
                    .withAsync(false)
                    .withCheckForSameOrigin(true)
                    .withEnricher(createTenantEnricher(PROJECT_1))
                    .withObjectPredicate(createAccessPredicate(GLOBAL_PROJECT, PROJECT_1))
                    .withWriteAccessValidator(createWriteAccessValidator(PROJECT_1))
                    .build();

            Instant timeBeforeUpdate = Instant.now();
            AutomationPackageUpdateResult resultAp1 = manager.createOrUpdateAutomationPackage(user1CreateApParams);
            Assert.assertEquals(CREATED, resultAp1.getStatus());

            // new library snapshot is uploaded in nexus
            providersResolver.getMavenArtifactMocks().put(libVersion1, new ResolvedMavenArtifact(
                    libJarUpdated,
                    new SnapshotMetadata("some timestamp", System.currentTimeMillis(), 1, true))
            );

            // 3.1. The managed library cannot be created with the same name
            try {
                manager.createAutomationPackageResource(ResourceManager.RESOURCE_TYPE_AP_MANAGED_LIBRARY, libSource, "testManagedLibrary", globalAdminParams);
                Assert.fail("Exception is not thrown");
            } catch (AutomationPackageManagerException ex){
                log.info("Caught: {}", ex.getMessage());
            }

            // 3.2 Global admin updates the library
            Resource updatedGlobalLibResource = manager.updateAutomationPackageManagedLibrary(
                    globalLibResource.getId().toString(), libSource, "testManagedLibraryUpdated", globalAdminParams
            );

            //Get latest verions of resource
            globalLibResource = resourceManager.getResource(globalLibResource.getId().toHexString());
            // updated library has the same id as the original one
            Assert.assertEquals(globalLibResource.getId(), updatedGlobalLibResource.getId());
            Assert.assertEquals("globalAdmin", globalLibResource.getLastModificationUser());
            Assert.assertTrue(globalLibResource.getLastModificationDate().toInstant().isAfter(timeBeforeUpdate));
            ResourceRevisionFileHandle actualLibRevision = resourceManager.getResourceFile(updatedGlobalLibResource.getId().toHexString());

            // actual resource revision contains updated lib
            Assert.assertArrayEquals(Files.readAllBytes(libJarUpdated.toPath()), Files.readAllBytes(actualLibRevision.getResourceFile().toPath()));

            // linked automation package is also updated (refreshed)
            AutomationPackage apAfterLibUpdate = automationPackageAccessor.get(resultAp1.getId());
            Assert.assertEquals(FileResolver.resolveResourceId(apAfterLibUpdate.getAutomationPackageLibraryResource()), updatedGlobalLibResource.getId().toHexString());
            Assert.assertTrue(apAfterLibUpdate.getLastModificationDate().toInstant().isAfter(timeBeforeUpdate));

            // 4. user without write permission cannot update the managed library
            try {
                manager.updateAutomationPackageManagedLibrary(
                        globalLibResource.getId().toString(), libSource, "testManagedLibraryUpdated", user1CreateApParams
                );
                Assert.fail("Exception is not thrown");
            } catch (AutomationPackageManagerException ex){
                log.info("Exception caught: {}", ex.getMessage());
            }
        } catch (IOException e) {
            throw new RuntimeException("IO Exception", e);
        }
    }

    @Test
    public void testManagedLibraryInIsolatedProjects() throws IOException, InterruptedException {
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);
        File anotherAutomationPackageJar = new File("src/test/resources/samples/" + SAMPLE_ECHO_FILE_NAME);

        File libJar = new File("src/test/resources/samples/" + KW_LIB_FILE_NAME);
        File libJarUpdated = new File("src/test/resources/samples/" + KW_LIB_FILE_UPDATED_NAME);

        MavenArtifactIdentifier libVersion1 = new MavenArtifactIdentifier("test-group", "test-lib", "1.0.0-SNAPSHOT", null, null);
        MockedAutomationPackageProvidersResolver providersResolver = (MockedAutomationPackageProvidersResolver) manager.getProvidersResolver();
        providersResolver.getMavenArtifactMocks().put(libVersion1, new ResolvedMavenArtifact(libJar, new SnapshotMetadata("some timestamp", System.currentTimeMillis(), 1, false)));

        AutomationPackageFileSource libSource = AutomationPackageFileSource.withMavenIdentifier(libVersion1);

        // 1. Create managed library in project1
        AutomationPackageUpdateParameter user1Params = new AutomationPackageUpdateParameterBuilder()
                .forJunit()
                .withActorUser("user1")
                .withEnricher(createTenantEnricher(PROJECT_1))
                .withObjectPredicate(createAccessPredicate(PROJECT_1))
                .withWriteAccessValidator(createWriteAccessValidator(PROJECT_1))
                .build();

        Resource projectLibResource1 = manager.createAutomationPackageResource(ResourceManager.RESOURCE_TYPE_AP_MANAGED_LIBRARY, libSource, "testManagedLibrary", user1Params);
        Assert.assertNotNull(projectLibResource1);
        Assert.assertEquals(PROJECT_1, projectLibResource1.getAttribute(ATTRIBUTE_PROJECT_NAME));

        // 2. Create managed library in project2 with the same name - it is allowed, because we use separate tenants
        AutomationPackageUpdateParameter user2Params = new AutomationPackageUpdateParameterBuilder()
                .forJunit()
                .withActorUser("user2")
                .withEnricher(createTenantEnricher(PROJECT_2))
                .withObjectPredicate(createAccessPredicate(PROJECT_2))
                .withWriteAccessValidator(createWriteAccessValidator(PROJECT_2))
                .build();

        Resource projectLibResource2 = manager.createAutomationPackageResource(ResourceManager.RESOURCE_TYPE_AP_MANAGED_LIBRARY, libSource, "testManagedLibrary", user2Params);
        Assert.assertNotNull(projectLibResource2);
        Assert.assertEquals(PROJECT_2, projectLibResource2.getAttribute(ATTRIBUTE_PROJECT_NAME));

        // 3. User1 cannot use the library from project2
        try (InputStream is = new FileInputStream(automationPackageJar)) {
            AutomationPackageFileSource sample1ApSource = AutomationPackageFileSource.withInputStream(is, SAMPLE1_FILE_NAME);
            try {
                AutomationPackageUpdateParameter user1CreateApParams = new AutomationPackageUpdateParameterBuilder()
                        .forJunit()
                        .withApSource(sample1ApSource)
                        .withApLibrarySource(AutomationPackageFileSource.withResourceId(projectLibResource2.getId().toHexString()))
                        .withAllowUpdate(false)
                        .withAsync(false)
                        .withCheckForSameOrigin(true)
                        .withEnricher(createTenantEnricher(PROJECT_1))
                        .withObjectPredicate(createAccessPredicate(PROJECT_1))
                        .withWriteAccessValidator(createWriteAccessValidator(PROJECT_1))
                        .build();

                manager.createOrUpdateAutomationPackage(user1CreateApParams);
                Assert.fail("Exception should be thrown");
            } catch (AutomationPackageManagerException ex) {
                log.info("Exception: {}", ex.getMessage());
            }
        } catch (IOException e) {
            throw new RuntimeException("IO Exception", e);
        }

        AutomationPackageUpdateResult resultAp1;
        AutomationPackageUpdateResult resultAp2;
        try (InputStream is = new FileInputStream(automationPackageJar);
             InputStream isAnother = new FileInputStream(anotherAutomationPackageJar)) {

            // 4. User1 and User2 can create automation packages referencing managed library within their own projects
            AutomationPackageFileSource sample1ApSource = AutomationPackageFileSource.withInputStream(is, SAMPLE1_FILE_NAME);

            AutomationPackageUpdateParameter user1CreateApParams = new AutomationPackageUpdateParameterBuilder()
                    .forJunit()
                    .withApSource(sample1ApSource)
                    .withApLibrarySource(AutomationPackageFileSource.withResourceId(projectLibResource1.getId().toHexString()))
                    .withAllowUpdate(false)
                    .withAsync(false)
                    .withCheckForSameOrigin(true)
                    .withEnricher(createTenantEnricher(PROJECT_1))
                    .withObjectPredicate(createAccessPredicate(PROJECT_1))
                    .withWriteAccessValidator(createWriteAccessValidator(PROJECT_1))
                    .build();

            resultAp1 = manager.createOrUpdateAutomationPackage(user1CreateApParams);
            Assert.assertEquals(CREATED, resultAp1.getStatus());
            AutomationPackageFileSource sample2ApSource = AutomationPackageFileSource.withInputStream(isAnother, SAMPLE1_FILE_NAME);

            AutomationPackageUpdateParameter user2CreateApParams = new AutomationPackageUpdateParameterBuilder()
                    .forJunit()
                    .withApSource(sample2ApSource)
                    .withApLibrarySource(AutomationPackageFileSource.withResourceId(projectLibResource2.getId().toHexString()))
                    .withAllowUpdate(false)
                    .withAsync(false)
                    .withCheckForSameOrigin(true)
                    .withEnricher(createTenantEnricher(PROJECT_2))
                    .withObjectPredicate(createAccessPredicate(PROJECT_2))
                    .withWriteAccessValidator(createWriteAccessValidator(PROJECT_2))
                    .build();

            resultAp2 = manager.createOrUpdateAutomationPackage(user2CreateApParams);
            Assert.assertEquals(CREATED, resultAp2.getStatus());
        } catch (IOException e) {
            throw new RuntimeException("IO Exception", e);
        }

        AutomationPackage ap1 = automationPackageAccessor.get(resultAp1.getId());
        AutomationPackage ap2 = automationPackageAccessor.get(resultAp2.getId());

        // check tenants linked with both APs
        Assert.assertEquals(PROJECT_1, ap1.getAttribute(ATTRIBUTE_PROJECT_NAME));
        Assert.assertEquals(PROJECT_2, ap2.getAttribute(ATTRIBUTE_PROJECT_NAME));

        // check references to libs
        Assert.assertEquals(FileResolver.resolveResourceId(ap1.getAutomationPackageLibraryResource()), projectLibResource1.getId().toHexString());
        Assert.assertEquals(FileResolver.resolveResourceId(ap2.getAutomationPackageLibraryResource()), projectLibResource2.getId().toHexString());

        // 5. User1 updates the lib in project1 - AP from project2 should be untouched
        providersResolver.getMavenArtifactMocks().put(libVersion1, new ResolvedMavenArtifact(
                libJarUpdated,
                new SnapshotMetadata("some timestamp", System.currentTimeMillis(), 1, true))
        );

        Instant nowBeforeLib1Update = Instant.now();
        Thread.sleep(1);
        RefreshResourceResult refreshResourceResult = manager.getAutomationPackageResourceManager().refreshResourceAndLinkedPackages(
                projectLibResource1.getId().toHexString(), user1Params, manager
        );
        Assert.assertEquals(RefreshResourceResult.ResultStatus.REFRESHED, refreshResourceResult.getResultStatus());

        // lib1 has been updated
        Resource updatedLib1Resource = resourceManager.getResource(projectLibResource1.getId().toHexString());
        Assert.assertFalse(updatedLib1Resource.getLastModificationDate().toInstant().isBefore(nowBeforeLib1Update));
        Assert.assertArrayEquals(Files.readAllBytes(resourceManager.getResourceFile(projectLibResource1.getId().toHexString()).getResourceFile().toPath()), Files.readAllBytes(libJarUpdated.toPath()));

        // lib2 is not updated
        Assert.assertFalse(projectLibResource2.getLastModificationDate().toInstant().isAfter(nowBeforeLib1Update));
        Assert.assertArrayEquals(Files.readAllBytes(resourceManager.getResourceFile(projectLibResource2.getId().toHexString()).getResourceFile().toPath()), Files.readAllBytes(libJar.toPath()));

        // take the actual state from db
        ap1 = automationPackageAccessor.get(ap1.getId());
        ap2 = automationPackageAccessor.get(ap2.getId());

        // ap1 has been reuploaded
        Assert.assertFalse(ap1.getLastModificationDate().toInstant().isBefore(nowBeforeLib1Update));

        // ap2 has not been reuploaded
        Assert.assertFalse(ap2.getLastModificationDate().toInstant().isAfter(nowBeforeLib1Update));

        // original tenants for automation packages should not be changed after reupload
        Assert.assertEquals(PROJECT_1, ap1.getAttribute(ATTRIBUTE_PROJECT_NAME));
        Assert.assertEquals(PROJECT_2, ap2.getAttribute(ATTRIBUTE_PROJECT_NAME));

        // 5. User1 still has the access to AP to read and delete it
        AutomationPackage apTakenFromManager = manager.getAutomationPackageById(ap1.getId(), createAccessPredicate(PROJECT_1));
        Assert.assertNotNull(apTakenFromManager);

        manager.removeAutomationPackage(ap1.getId(), "user1", createAccessPredicate(PROJECT_1), createWriteAccessValidator(PROJECT_1));

        // ap1 doesn't exist anymore
        Assert.assertNull(automationPackageAccessor.get(ap1.getId()));

        //Library resources were created manually, they shall not be deleted when deleting APs using them
        resourceManager.getResourceFile(updatedLib1Resource.getId().toString());
        resourceManager.getResourceFile(projectLibResource2.getId().toString());
        try {
            manager.getAutomationPackageResourceManager().deleteResource(updatedLib1Resource.getId().toString(), user1Params.writeAccessValidator);
        } catch (AutomationPackageUnsupportedResourceTypeException e) {
            log.error("Unable to delete library", e);
            Assert.fail("Unable to delete library");
        }
        try {
            resourceManager.getResourceFile(updatedLib1Resource.getId().toString());
            Assert.fail("Exception should be thrown");
        } catch (ResourceMissingException ex){
            log.info("Resource deleted: {}", ex.getMessage());
        }
    }

    protected AutomationPackageManager createManager(AutomationPackageHookRegistry automationPackageHookRegistry, AutomationPackageReaderRegistry automationPackageReaderRegistry) {
        ObjectHookRegistry objectHookRegistry = new ObjectHookRegistry();
        objectHookRegistry.add(new ObjectHook() {
            @Override
            public ObjectFilter getObjectFilter(AbstractContext context) {
                // TODO: maybe we need to mock the object filter also
                return null;
            }

            @Override
            public ObjectEnricher getObjectEnricher(AbstractContext context) {
                return createTenantEnricher(context.get("project") == null ? null : (String) context.get("project"));
            }

            @Override
            public void rebuildContext(AbstractContext context, EnricheableObject object) throws Exception {
                context.put("project", object.getAttribute(ATTRIBUTE_PROJECT_NAME));
            }
        });

        return AutomationPackageManager.createMainAutomationPackageManager(
                automationPackageAccessor,
                functionManager,
                functionAccessor,
                planAccessor,
                resourceManager,
                automationPackageHookRegistry,
                automationPackageReaderRegistry,
                automationPackageLocks,
                null, -1,
                objectHookRegistry
        );
    }

    protected WriteAccessValidator createWriteAccessValidator(String ... projectNames){
        return new WriteAccessValidator() {
            @Override
            public void validate(EnricheableObject entity) throws ObjectAccessException {
                if(!createAccessPredicate(projectNames).test(entity)){
                    throw new ObjectAccessException(List.of(new ObjectAccessViolation("testHookId", "testErrorCode", "User has no access to " + entity.toString())));
                }
            }
        };
    }

    protected ObjectPredicate createAccessPredicate(String ... projectNames) {
        return object -> {
            String projectNameAttr = object.getAttribute(ATTRIBUTE_PROJECT_NAME);
            return projectNameAttr != null && Arrays.asList(projectNames).contains(projectNameAttr);
        };
    }

    protected ObjectEnricher createTenantEnricher(String projectName) {
        return new ObjectEnricher() {

            @Override
            public void accept(EnricheableObject t) {
                enrichObject(t);
            }

            @Override
            public TreeMap<String, String> getAdditionalAttributes() {
                TreeMap<String, String> additionalAttributes = new TreeMap<>();
                if (projectName != null) {
                    additionalAttributes.put(ATTRIBUTE_PROJECT_NAME, projectName);
                }
                return additionalAttributes;
            }

            private void enrichObject(EnricheableObject object) {
                if (object != null) {
                    if (projectName != null) {
                        Map<String, String> attributes = object.getAttributes();
                        if (attributes == null) {
                            attributes = new HashMap<>();
                            object.setAttributes(attributes);
                        }
                        attributes.put(ATTRIBUTE_PROJECT_NAME, projectName);
                    }
                }

            }
        };
    }

}
