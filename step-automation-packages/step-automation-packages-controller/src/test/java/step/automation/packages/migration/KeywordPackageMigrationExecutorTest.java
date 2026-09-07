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
package step.automation.packages.migration;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import step.attachments.FileResolver;
import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageUpdateParameter;
import step.automation.packages.AutomationPackageUpdateResult;
import step.automation.packages.AutomationPackageUpdateStatus;
import step.automation.packages.AutomationPackageEntity;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.automation.packages.accessor.AutomationPackageAccessorImpl;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.entities.EntityConstants;
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectHook;
import step.core.objectenricher.ObjectHookRegistry;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.FunctionAccessorImpl;
import step.plugins.java.GeneralScriptFunction;
import step.resources.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static step.automation.packages.migration.KeywordPackageClassifier.EMBEDDED_PACKAGE_CUSTOM_FIELD;
import static step.automation.packages.migration.KeywordPackageMigrationExecutor.FUNCTION_PACKAGE_ID_CUSTOM_FIELD;
import static step.automation.packages.migration.KeywordPackageMigrationTask.STAGING_COLLECTION;

/**
 * Covers what the plugin phase decides — which mode branch runs, which resources are re-typed, what is
 * deleted, and what the automation package manager is asked to deploy. The manager's own behaviour is
 * covered by its own tests, so it is mocked here; everything else is a real accessor.
 */
public class KeywordPackageMigrationExecutorTest {

    /** Multi-tenancy stamps ownership here; the constant itself lives in the enterprise tree. */
    private static final String PROJECT_ATTRIBUTE = "project";

    private CollectionFactory collectionFactory;
    private Collection<StagedKeywordPackage> staging;
    private FunctionAccessor functionAccessor;
    private AutomationPackageAccessor automationPackageAccessor;
    private ResourceManager resourceManager;
    private AutomationPackageManager automationPackageManager;
    private ObjectHookRegistry objectHookRegistry;
    private Path resourceRoot;

    @Before
    public void setUp() throws Exception {
        collectionFactory = new InMemoryCollectionFactory(null);
        staging = collectionFactory.getCollection(STAGING_COLLECTION, StagedKeywordPackage.class);
        functionAccessor = new FunctionAccessorImpl(
                collectionFactory.getCollection(EntityConstants.functions, Function.class));
        automationPackageAccessor = new AutomationPackageAccessorImpl(
                collectionFactory.getCollection(AutomationPackageEntity.entityName, AutomationPackage.class));

        resourceRoot = Files.createTempDirectory("kpm-resources");
        resourceManager = new LocalResourceManagerImpl(resourceRoot.toFile(),
                new ResourceAccessorImpl(collectionFactory.getCollection(EntityConstants.resources, Resource.class)),
                new ResourceRevisionAccessorImpl(
                        collectionFactory.getCollection("resourceRevisions", ResourceRevision.class)));

        automationPackageManager = Mockito.mock(AutomationPackageManager.class);
        Mockito.when(automationPackageManager.createOrUpdateAutomationPackage(Mockito.any()))
                .thenReturn(new AutomationPackageUpdateResult(AutomationPackageUpdateStatus.CREATED,
                        new ObjectId(), null, null));

        objectHookRegistry = new ObjectHookRegistry();
    }

    @After
    public void tearDown() {
        resourceManager.cleanup();
    }

    // -------------------------------------------------------------- migrate

    @Test
    public void aResourceBackedPackageIsDeployedAndItsKeywordsRemoved() throws Exception {
        Resource archive = createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, "payments.jar");
        StagedKeywordPackage staged = stage("payments", FileResolver.createPathForResource(archive));
        staged.setPackageAttributes(Map.of("team", "billing"));
        staged.setTokenSelectionCriteria(Map.of("os", "linux"));
        staged.setExecuteLocally(true);
        staging.save(staged);
        keyword(staged.getId());

        run(KeywordPackageMigrationMode.MIGRATE);

        assertEquals("the old keywords are replaced by the ones the manager rebuilds", 0, countKeywords());
        assertEquals("the staging record is cleared once handled", 0, count(staging));

        AutomationPackageUpdateParameter parameters = captureDeployment();
        assertEquals(Map.of("team", "billing"), parameters.functionsAttributes);
        assertEquals(Map.of("os", "linux"), parameters.tokenSelectionCriteria);
        assertTrue(parameters.executionFunctionsLocally);
        assertTrue(parameters.allowCreate);
        assertFalse(parameters.allowUpdate);
        assertFalse("the migration must finish before startup continues", parameters.async);
    }

    /**
     * The point of re-typing rather than re-uploading: the automation package ends up on the very same
     * resource, so no copy of the archive is made and every stored reference stays valid.
     */
    @Test
    public void theExistingResourceIsRetypedAndReused() throws Exception {
        Resource archive = createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, "payments.jar");
        staging.save(stage("payments", FileResolver.createPathForResource(archive)));

        run(KeywordPackageMigrationMode.MIGRATE);

        assertEquals(ResourceManager.RESOURCE_TYPE_AP,
                resourceManager.getResource(archive.getId().toString()).getResourceType());
        assertEquals(archive.getId().toString(), captureDeployment().apSource.getResourceId());
    }

    @Test
    public void librariesAreRetypedAndPassedAsTheLibrarySource() throws Exception {
        Resource archive = createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, "payments.jar");
        Resource libraries = createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, "libs.zip");
        StagedKeywordPackage staged = stage("payments", FileResolver.createPathForResource(archive));
        staged.setPackageLibrariesLocation(FileResolver.createPathForResource(libraries));
        staging.save(staged);

        run(KeywordPackageMigrationMode.MIGRATE);

        assertEquals(ResourceManager.RESOURCE_TYPE_AP_LIBRARY,
                resourceManager.getResource(libraries.getId().toString()).getResourceType());
        assertEquals(libraries.getId().toString(), captureDeployment().apLibrarySource.getResourceId());
    }

    @Test
    public void aPathLocatedPackageIsDeployedFromTheFileOnDisk() throws Exception {
        File archiveOnDisk = Files.write(resourceRoot.resolve("payments.jar"),
                "jar".getBytes(StandardCharsets.UTF_8)).toFile();
        staging.save(stage("payments", archiveOnDisk.getAbsolutePath()));

        run(KeywordPackageMigrationMode.MIGRATE);

        assertNull("no resource existed, so none may be reused", captureDeployment().apSource.getResourceId());
    }

    /**
     * The name must be passed explicitly, because an automation package is otherwise named after its
     * archive — which, for a reused resource, this migration does not control.
     */
    @Test
    public void theKeywordPackageNameIsCarriedOverAsTheArchiveName() throws Exception {
        Resource archive = createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, "payments-1.2.3.jar");
        staging.save(stage("payments", FileResolver.createPathForResource(archive)));

        run(KeywordPackageMigrationMode.MIGRATE);

        assertEquals("payments", captureDeployment().apSource.getArchiveName());
    }

    @Test
    public void aCollidingNameGetsACounter() throws Exception {
        automationPackageAccessor.save(automationPackage("payments"));
        Resource archive = createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, "payments.jar");
        staging.save(stage("payments", FileResolver.createPathForResource(archive)));

        run(KeywordPackageMigrationMode.MIGRATE);

        assertEquals("payments (1)", captureDeployment().apSource.getArchiveName());
    }

    /**
     * The enricher and the predicate both come from the hook registry applied to the staged package, so
     * every hook contributes rather than only project ownership.
     */
    @Test
    public void enrichmentIsRebuiltFromTheStagedPackageThroughTheHooks() throws Exception {
        String projectId = new ObjectId().toHexString();
        objectHookRegistry.add(projectStampingHook());

        Resource archive = createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, "payments.jar");
        StagedKeywordPackage staged = stage("payments", FileResolver.createPathForResource(archive));
        staged.addAttribute(PROJECT_ATTRIBUTE, projectId);
        staging.save(staged);

        run(KeywordPackageMigrationMode.MIGRATE);

        EnricheableTestObject enriched = new EnricheableTestObject();
        captureDeployment().enricher.accept(enriched);
        assertEquals(projectId, enriched.getAttributes().get(PROJECT_ATTRIBUTE));
    }

    @Test
    public void aFailedDeploymentClearsItsRecordAndDoesNotStopTheRest() throws Exception {
        Mockito.when(automationPackageManager.createOrUpdateAutomationPackage(Mockito.any()))
                .thenThrow(new RuntimeException("archive cannot be read"))
                .thenReturn(new AutomationPackageUpdateResult(AutomationPackageUpdateStatus.CREATED,
                        new ObjectId(), null, null));

        Resource first = createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, "first.jar");
        Resource second = createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, "second.jar");
        staging.save(stage("first", FileResolver.createPathForResource(first)));
        staging.save(stage("second", FileResolver.createPathForResource(second)));

        run(KeywordPackageMigrationMode.MIGRATE);

        assertEquals("both records are cleared, the failed one is not retried", 0, count(staging));
        Mockito.verify(automationPackageManager, Mockito.times(2))
                .createOrUpdateAutomationPackage(Mockito.any());
    }

    @Test
    public void embeddedPackagesAreRemovedAndNothingIsDeployed() throws Exception {
        StagedKeywordPackage staged = stage("enterprise-functions", "/opt/step/kw/embedded.jar");
        staged.addCustomField(EMBEDDED_PACKAGE_CUSTOM_FIELD, "embedded.jar");
        staging.save(staged);
        keyword(staged.getId());

        run(KeywordPackageMigrationMode.MIGRATE);

        assertEquals("their keywords would collide with the ones the embedded feature recreates",
                0, countKeywords());
        Mockito.verify(automationPackageManager, Mockito.never()).createOrUpdateAutomationPackage(Mockito.any());
    }

    @Test
    public void packagesWhoseFileIsMissingAreDroppedWithoutAReplacement() throws Exception {
        StagedKeywordPackage staged = staging.save(stage("payments", "/does/not/exist/payments.jar"));
        keyword(staged.getId());

        run(KeywordPackageMigrationMode.MIGRATE);

        assertEquals(0, countKeywords());
        Mockito.verify(automationPackageManager, Mockito.never()).createOrUpdateAutomationPackage(Mockito.any());
    }

    // --------------------------------------------------------------- detach

    @Test
    public void detachKeepsTheKeywordsAndTheirResourceUntouched() throws Exception {
        Resource archive = createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, "payments.jar");
        StagedKeywordPackage staged = staging.save(stage("payments", FileResolver.createPathForResource(archive)));
        ObjectId keywordId = keyword(staged.getId()).getId();

        run(KeywordPackageMigrationMode.DETACH);

        assertEquals(1, countKeywords());
        assertEquals("the resource keeps backing the surviving keywords",
                ResourceManager.RESOURCE_TYPE_FUNCTIONS,
                resourceManager.getResource(archive.getId().toString()).getResourceType());
        assertNull("the reference would point at an entity that no longer exists",
                functionAccessor.get(keywordId).getCustomField(FUNCTION_PACKAGE_ID_CUSTOM_FIELD));
        Mockito.verify(automationPackageManager, Mockito.never()).createOrUpdateAutomationPackage(Mockito.any());
    }

    // --------------------------------------------------------------- delete

    @Test
    public void deleteRemovesTheKeywordsAndTheResources() throws Exception {
        Resource archive = createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, "payments.jar");
        StagedKeywordPackage staged = staging.save(stage("payments", FileResolver.createPathForResource(archive)));
        keyword(staged.getId());

        run(KeywordPackageMigrationMode.DELETE);

        assertEquals(0, countKeywords());
        // getResource raises rather than returning null for a resource that is gone.
        assertThrows(ResourceMissingException.class,
                () -> resourceManager.getResource(archive.getId().toString()));
        Mockito.verify(automationPackageManager, Mockito.never()).createOrUpdateAutomationPackage(Mockito.any());
    }

    /**
     * A resource-backed package whose resource has been deleted cannot be converted, and its keywords
     * could not have run either way. It is handled like any other unmigratable package rather than
     * stopping the run.
     */
    @Test
    public void aPackageWhoseResourceIsGoneIsDroppedRatherThanFailingTheRun() throws Exception {
        Resource archive = createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, "payments.jar");
        StagedKeywordPackage staged = staging.save(stage("payments", FileResolver.createPathForResource(archive)));
        keyword(staged.getId());
        resourceManager.deleteResource(archive.getId().toString());

        run(KeywordPackageMigrationMode.MIGRATE);

        assertEquals("the record must not be left behind for a retry", 0, count(staging));
        Mockito.verify(automationPackageManager, Mockito.never()).createOrUpdateAutomationPackage(Mockito.any());
    }

    @Test
    public void anEmptyStagingCollectionIsANoOp() {
        run(KeywordPackageMigrationMode.MIGRATE);

        Mockito.verifyNoInteractions(automationPackageManager);
    }

    // --------------------------------------------------------------- helpers

    private void run(KeywordPackageMigrationMode mode) {
        new KeywordPackageMigrationExecutor(staging, functionAccessor, automationPackageAccessor,
                automationPackageManager, resourceManager, objectHookRegistry, mode).run();
    }

    private AutomationPackageUpdateParameter captureDeployment() {
        ArgumentCaptor<AutomationPackageUpdateParameter> captor =
                ArgumentCaptor.forClass(AutomationPackageUpdateParameter.class);
        Mockito.verify(automationPackageManager).createOrUpdateAutomationPackage(captor.capture());
        return captor.getValue();
    }

    private Resource createResource(String type, String fileName) throws Exception {
        return resourceManager.createResource(type,
                new ByteArrayInputStream("jar".getBytes(StandardCharsets.UTF_8)), fileName, null, "test");
    }

    private StagedKeywordPackage stage(String name, String packageLocation) {
        StagedKeywordPackage staged = new StagedKeywordPackage();
        staged.addAttribute(AbstractOrganizableObject.NAME, name);
        staged.setPackageLocation(packageLocation);
        return staged;
    }

    private AutomationPackage automationPackage(String name) {
        AutomationPackage automationPackage = new AutomationPackage();
        automationPackage.addAttribute(AbstractOrganizableObject.NAME, name);
        return automationPackage;
    }

    private Function keyword(ObjectId keywordPackageId) {
        GeneralScriptFunction keyword = new GeneralScriptFunction();
        keyword.addAttribute(AbstractOrganizableObject.NAME, "Login");
        keyword.addCustomField(FUNCTION_PACKAGE_ID_CUSTOM_FIELD, keywordPackageId.toString());
        return functionAccessor.save(keyword);
    }

    private long countKeywords() {
        return functionAccessor.stream().count();
    }

    private long count(Collection<?> collection) {
        return collection.find(Filters.empty(), null, null, null, 0).count();
    }

    /**
     * Stands in for the multi-tenancy hook: rebuilds a context from the object's own project attribute
     * and hands back an enricher that stamps it, which is the behaviour the executor relies on.
     */
    private ObjectHook projectStampingHook() {
        return new ObjectHook() {
            @Override
            public step.core.objectenricher.ObjectFilter getObjectFilter(step.core.AbstractContext context) {
                return () -> "";
            }

            @Override
            public ObjectEnricher getObjectEnricher(step.core.AbstractContext context) {
                String projectId = (String) context.get("projectId");
                return object -> {
                    if (projectId == null) {
                        return;
                    }
                    Map<String, String> attributes = object.getAttributes();
                    if (attributes == null) {
                        attributes = new HashMap<>();
                        object.setAttributes(attributes);
                    }
                    attributes.put(PROJECT_ATTRIBUTE, projectId);
                };
            }

            @Override
            public void rebuildContext(step.core.AbstractContext context, EnricheableObject object) {
                context.put("projectId", object.getAttributes() == null ? null
                        : object.getAttributes().get(PROJECT_ATTRIBUTE));
            }
        };
    }

    private static class EnricheableTestObject extends AbstractOrganizableObject implements EnricheableObject {
    }
}
