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

import jakarta.validation.constraints.AssertTrue;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.core.maven.MavenArtifactIdentifier;
import step.core.objectenricher.*;
import step.repositories.artifact.ResolvedMavenArtifact;
import step.repositories.artifact.SnapshotMetadata;
import step.resources.Resource;
import step.resources.ResourceMissingException;

import java.io.*;
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

            // TODO: is it OK that the AP linked with project1 is automatically refreshed without write access to project1?
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
