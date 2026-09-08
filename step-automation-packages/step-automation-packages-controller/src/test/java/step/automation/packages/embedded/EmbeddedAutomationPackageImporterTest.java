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
package step.automation.packages.embedded;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageUpdateParameter;
import step.automation.packages.AutomationPackageUpdateResult;
import step.automation.packages.AutomationPackageUpdateStatus;
import step.core.AbstractContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.AttributeResolverRegistry;
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectFilter;
import step.core.objectenricher.ObjectHook;
import step.core.objectenricher.ObjectHookRegistry;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

/**
 * Covers what the importer decides — which files are treated as packages, what the sidecar
 * contributes, and what the automation package manager is asked to deploy. The manager is mocked, as
 * its own behaviour and the reading of a descriptor-less archive have their own tests.
 */
public class EmbeddedAutomationPackageImporterTest {

    /** Multi-tenancy stamps ownership here; the constant itself lives in the enterprise tree. */
    private static final String PROJECT_ATTRIBUTE = "project";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private AutomationPackageManager automationPackageManager;
    private ObjectHookRegistry objectHookRegistry;
    private AttributeResolverRegistry attributeResolverRegistry;

    @Before
    public void setUp() throws Exception {
        automationPackageManager = Mockito.mock(AutomationPackageManager.class);
        Mockito.when(automationPackageManager.createOrUpdateAutomationPackage(Mockito.any()))
                .thenAnswer(invocation -> new AutomationPackageUpdateResult(
                        AutomationPackageUpdateStatus.CREATED, new ObjectId(), null, null));

        objectHookRegistry = new ObjectHookRegistry();
        attributeResolverRegistry = new AttributeResolverRegistry();
    }

    @Test
    public void archivesOfBothFoldersAreDeployedWithTheMatchingExecutionMode() throws Exception {
        archive("local", "controller-keywords.jar");
        archive("remote", "agent-keywords.jar");

        List<String> deployed = run();

        assertEquals(2, deployed.size());
        Map<String, AutomationPackageUpdateParameter> byName = captureDeploymentsByFileName();
        assertTrue("a package of the local folder runs on the controller",
                byName.get("controller-keywords.jar").executionFunctionsLocally);
        assertFalse("a package of the remote folder runs on an agent",
                byName.get("agent-keywords.jar").executionFunctionsLocally);
    }

    @Test
    public void aPackageAlreadyDeployedIsUpdatedRatherThanDuplicated() throws Exception {
        archive("local", "keywords.jar");

        run();

        AutomationPackageUpdateParameter parameters = captureDeployment();
        assertTrue("an archive shipped again has to update the package it deployed before",
                parameters.allowUpdate);
        assertTrue(parameters.allowCreate);
        assertFalse("the import must finish before startup continues", parameters.async);
    }

    /**
     * The archive is recognized by its content, so the sidecar and anything else the folder holds are
     * left out without relying on a naming convention.
     */
    @Test
    public void onlyArchivesAreTreatedAsPackages() throws Exception {
        archive("local", "keywords.jar");
        sidecar("local", "keywords.jar", "{\"attributes\": {\"project\": \"@Common\"}}");
        Files.writeString(new File(folder.getRoot(), "local/notes.txt").toPath(), "not an archive");
        Files.writeString(new File(folder.getRoot(), "local/broken.jar").toPath(), "not an archive either");
        assertTrue(new File(folder.getRoot(), "local/nested").mkdirs());

        assertEquals(1, run().size());
    }

    @Test
    public void aSidecarProjectIsResolvedAndAppliedToTheDeployedPackage() throws Exception {
        objectHookRegistry.add(projectHook());
        attributeResolverRegistry.register(PROJECT_ATTRIBUTE, name -> "id-of-" + name);
        archive("local", "keywords.jar");
        sidecar("local", "keywords.jar", "{\"attributes\": {\"project\": \"@Common\"}}");

        run();

        AutomationPackageUpdateParameter parameters = captureDeployment();
        EnricheableTestObject deployedEntity = new EnricheableTestObject();
        parameters.enricher.accept(deployedEntity);
        assertEquals("the symbolic project name has to reach the package as the resolved id",
                "id-of-Common", deployedEntity.getAttribute(PROJECT_ATTRIBUTE));

        assertTrue("the lookup of the package to update is scoped to the same project",
                parameters.objectPredicate.test(entityOfProject("id-of-Common")));
        assertFalse(parameters.objectPredicate.test(entityOfProject("id-of-Other")));
    }

    @Test
    public void anArchiveWithoutSidecarIsDeployedUnscoped() throws Exception {
        objectHookRegistry.add(projectHook());
        archive("local", "keywords.jar");

        assertEquals(1, run().size());

        EnricheableTestObject deployedEntity = new EnricheableTestObject();
        captureDeployment().enricher.accept(deployedEntity);
        assertNull(deployedEntity.getAttribute(PROJECT_ATTRIBUTE));
    }

    /**
     * Unknown properties are ignored so that the sidecars written against the keyword package model
     * keep working, of which only the attributes ever applied.
     */
    @Test
    public void aSidecarKeepsWorkingWithTheFieldsOfTheKeywordPackageModel() throws Exception {
        objectHookRegistry.add(projectHook());
        attributeResolverRegistry.register(PROJECT_ATTRIBUTE, name -> "id-of-" + name);
        archive("local", "keywords.jar");
        sidecar("local", "keywords.jar",
                "{\"attributes\": {\"project\": \"@Common\"}, \"executeLocally\": true, \"packageLocation\": \"/tmp/x\"}");

        assertEquals(1, run().size());

        EnricheableTestObject deployedEntity = new EnricheableTestObject();
        captureDeployment().enricher.accept(deployedEntity);
        assertEquals("id-of-Common", deployedEntity.getAttribute(PROJECT_ATTRIBUTE));
    }

    @Test
    public void aMissingFolderIsNotAnError() {
        assertTrue(run().isEmpty());
    }

    @Test
    public void oneFailingArchiveDoesNotStopTheOthers() throws Exception {
        archive("local", "broken-keywords.jar");
        archive("local", "sound-keywords.jar");
        Mockito.when(automationPackageManager.createOrUpdateAutomationPackage(Mockito.any()))
                .thenAnswer(invocation -> {
                    AutomationPackageUpdateParameter parameters = invocation.getArgument(0);
                    if (parameters.apSource.getFileName().startsWith("broken")) {
                        throw new RuntimeException("cannot be read");
                    }
                    return new AutomationPackageUpdateResult(AutomationPackageUpdateStatus.CREATED,
                            new ObjectId(), null, null);
                });

        assertEquals("the sound package is still deployed", 1, run().size());
    }

    // ------------------------------------------------------------------ setup

    private List<String> run() {
        return new EmbeddedAutomationPackageImporter(automationPackageManager, objectHookRegistry,
                attributeResolverRegistry).importEmbeddedAutomationPackages(folder.getRoot().getAbsolutePath());
    }

    /** A minimal archive: the importer recognizes it by its ZIP header, not by its content. */
    private void archive(String subFolder, String fileName) throws Exception {
        File file = new File(folder.getRoot(), subFolder + "/" + fileName);
        assertTrue(file.getParentFile().exists() || file.getParentFile().mkdirs());
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zip.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    private void sidecar(String subFolder, String archiveName, String content) throws Exception {
        Files.writeString(new File(folder.getRoot(), subFolder + "/" + archiveName + ".json").toPath(), content);
    }

    private AutomationPackageUpdateParameter captureDeployment() throws Exception {
        ArgumentCaptor<AutomationPackageUpdateParameter> captor =
                ArgumentCaptor.forClass(AutomationPackageUpdateParameter.class);
        Mockito.verify(automationPackageManager).createOrUpdateAutomationPackage(captor.capture());
        return captor.getValue();
    }

    private Map<String, AutomationPackageUpdateParameter> captureDeploymentsByFileName() throws Exception {
        ArgumentCaptor<AutomationPackageUpdateParameter> captor =
                ArgumentCaptor.forClass(AutomationPackageUpdateParameter.class);
        Mockito.verify(automationPackageManager, Mockito.atLeastOnce())
                .createOrUpdateAutomationPackage(captor.capture());
        Map<String, AutomationPackageUpdateParameter> byName = new HashMap<>();
        captor.getAllValues().forEach(parameters -> byName.put(parameters.apSource.getFileName(), parameters));
        return byName;
    }

    private EnricheableTestObject entityOfProject(String projectId) {
        EnricheableTestObject object = new EnricheableTestObject();
        object.addAttribute(PROJECT_ATTRIBUTE, projectId);
        return object;
    }

    /** Stands in for the enterprise multi-tenancy hook, which is what stamps and scopes by project. */
    private ObjectHook projectHook() {
        return new ObjectHook() {
            @Override
            public ObjectFilter getObjectFilter(AbstractContext context) {
                String projectId = (String) context.get("projectId");
                return () -> projectId == null ? "" : "attributes." + PROJECT_ATTRIBUTE + " = " + projectId;
            }

            @Override
            public ObjectEnricher getObjectEnricher(AbstractContext context) {
                String projectId = (String) context.get("projectId");
                return object -> {
                    if (projectId != null) {
                        Map<String, String> attributes = object.getAttributes();
                        if (attributes == null) {
                            attributes = new HashMap<>();
                            object.setAttributes(attributes);
                        }
                        attributes.put(PROJECT_ATTRIBUTE, projectId);
                    }
                };
            }

            @Override
            public void rebuildContext(AbstractContext context, EnricheableObject object) {
                // Mirrors the enterprise hook, which falls back to the global tenant rather than
                // failing when the object carries no project.
                String projectId = object.getAttributes() == null ? null
                        : object.getAttributes().get(PROJECT_ATTRIBUTE);
                if (projectId != null) {
                    context.put("projectId", projectId);
                }
            }
        };
    }

    private static class EnricheableTestObject extends AbstractOrganizableObject implements EnricheableObject {
    }
}
