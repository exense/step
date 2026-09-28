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
package step.automation.packages.execution;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.automation.packages.AbstractAutomationPackageManagerTest;
import step.automation.packages.execution.RepositoryWithAutomationPackageSupport.AutomationPackageFile;
import step.automation.packages.execution.RepositoryWithAutomationPackageSupport.IsolatedPackageExecutionContext;
import step.automation.packages.execution.RepositoryWithAutomationPackageSupport.PackageExecutionContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.model.Execution;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.resources.ResourceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static step.automation.packages.execution.RepositoryWithAutomationPackageSupport.AP_NAME;
import static step.automation.packages.execution.RepositoryWithAutomationPackageSupport.REPOSITORY_PARAM_CONTEXTID;

public class IsolatedPackageExecutionContextTest extends AbstractAutomationPackageManagerTest {

    private static final ObjectPredicate ALL_OBJECTS = o -> true;
    private static final ObjectEnricher NO_ENRICHMENT = o -> {
    };

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File isolatedRoot;
    private File stagingRoot;
    private IsolatedAutomationPackageRepository repository;
    private AutomationPackageFile apFile;

    @Before
    public void beforeIsolated() throws IOException {
        isolatedRoot = temporaryFolder.newFolder("temp_isolated_ap");
        stagingRoot = temporaryFolder.newFolder("temp_staging_ap");
        manager.setIsolatedResourcesRoot(isolatedRoot);
        manager.setStagingResourcesRoot(stagingRoot);
        repository = new IsolatedAutomationPackageRepository(manager, resourceManager, functionTypeRegistry, functionAccessor, () -> null, null) {
        };
        apFile = new AutomationPackageFile(new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME), null);
    }

    @Test
    public void contextsWithSameIdUseDistinctFolders() throws IOException {
        List<String> workingDirectoryEntriesBefore = listTemporaryFoldersInWorkingDirectory();
        String contextId = new ObjectId().toString();

        String sharedContextId1 = new ObjectId().toString();
        String sharedContextId2 = new ObjectId().toString();
        PackageExecutionContext context1 = createContext(contextId, sharedContextId1, false);
        PackageExecutionContext context2 = createContext(contextId, sharedContextId2, false);

        List<String> folders = listFolders(isolatedRoot).stream().map(File::getName).sorted().collect(Collectors.toList());
        assertEquals(Stream.of(sharedContextId1, sharedContextId2).map(id -> "resources_" + contextId + "_" + id).sorted().collect(Collectors.toList()), folders);
        assertEquals(workingDirectoryEntriesBefore, listTemporaryFoldersInWorkingDirectory());

        // closing the first context must not delete the files of the second one
        context1.close();
        List<File> remainingFolders = listFolders(isolatedRoot);
        assertEquals(1, remainingFolders.size());
        assertTrue(containsJar(remainingFolders.get(0)));

        context2.close();
        assertEquals(0, listFolders(isolatedRoot).size());
    }

    @Test
    public void sharedContextIsOnlyUsedWithItsSharedContextId() throws IOException {
        ObjectId contextId = new ObjectId();
        String sharedContextId = new ObjectId().toString();
        IsolatedPackageExecutionContext sharedContext = createStoredSharedContext(contextId, sharedContextId);
        Map<String, String> repositoryParameters = getRepositoryParameters(contextId, sharedContext);

        assertSame(sharedContext, repository.getOrRestorePackageExecutionContext(sharedContextId, repositoryParameters, null, ALL_OBJECTS, null));

        // a re-execution with the same context id but without shared context id gets its own context
        IsolatedPackageExecutionContext reExecutionContext = (IsolatedPackageExecutionContext) repository.getOrRestorePackageExecutionContext(
            null, repositoryParameters, null, ALL_OBJECTS, null);
        assertNotSame(sharedContext, reExecutionContext);
        assertFalse(reExecutionContext.isShared());
        assertNotEquals(sharedContextId, reExecutionContext.getSharedContextId());
        assertTrue(new File(isolatedRoot, "resources_" + contextId + "_" + reExecutionContext.getSharedContextId()).isDirectory());

        // the end of the shared context doesn't affect the re-execution
        sharedContext.close();
        assertNull(repository.sharedPackageExecutionContexts.get(sharedContextId));
        List<File> remainingFolders = listFolders(isolatedRoot);
        assertEquals(1, remainingFolders.size());
        assertTrue(containsJar(remainingFolders.get(0)));

        reExecutionContext.close();
        assertEquals(0, listFolders(isolatedRoot).size());
    }

    @Test
    public void sharedContextIdIsConsumedByTheImport() throws IOException {
        ObjectId contextId = new ObjectId();
        String sharedContextId = new ObjectId().toString();
        IsolatedPackageExecutionContext sharedContext = createStoredSharedContext(contextId, sharedContextId);

        try (ExecutionContext executionContext = ExecutionEngine.builder().build().newExecutionContext()) {
            executionContext.put(FunctionAccessor.class, new InMemoryFunctionAccessorImpl());
            executionContext.getExecutionParameters().setSharedContextId(sharedContextId);
            Execution execution = new Execution();
            execution.setId(new ObjectId(executionContext.getExecutionId()));
            execution.setExecutionParameters(executionContext.getExecutionParameters());
            executionContext.getExecutionAccessor().save(execution);

            repository.importArtefact(executionContext, getRepositoryParameters(contextId, sharedContext));

            // the shared context was used: it is closed by its creator and not with the execution
            assertNull(executionContext.get(PackageExecutionContext.class));
            assertNull(executionContext.getExecutionParameters().getSharedContextId());
            assertNull(executionContext.getExecutionAccessor().get(executionContext.getExecutionId()).getExecutionParameters().getSharedContextId());
        } finally {
            sharedContext.close();
        }
    }

    /**
     * Stores the AP as the executor does, to support the restore of the re-executions, and creates the shared context
     */
    private IsolatedPackageExecutionContext createStoredSharedContext(ObjectId contextId, String sharedContextId) throws IOException {
        AutomationPackageFile storedApFile;
        try (InputStream is = new FileInputStream(apFile.getFile())) {
            storedApFile = repository.getApFileForExecution(is, apFile.getFile().getName(), null, contextId, NO_ENRICHMENT,
                ALL_OBJECTS, "test", ResourceManager.RESOURCE_TYPE_ISOLATED_AP);
        }
        IsolatedPackageExecutionContext sharedContext = repository.createIsolatedPackageExecutionContext(NO_ENRICHMENT, ALL_OBJECTS,
            contextId.toString(), sharedContextId, storedApFile, true, null, "test");
        repository.setApNameForResource(storedApFile.getResource(), getApName(sharedContext));
        return sharedContext;
    }

    private static Map<String, String> getRepositoryParameters(ObjectId contextId, PackageExecutionContext context) {
        return Map.of(REPOSITORY_PARAM_CONTEXTID, contextId.toString(), AP_NAME, getApName(context));
    }

    private static String getApName(PackageExecutionContext context) {
        return context.getAutomationPackage().getAttribute(AbstractOrganizableObject.NAME);
    }

    private IsolatedPackageExecutionContext createContext(String contextId, String sharedContextId, boolean shared) {
        return repository.createIsolatedPackageExecutionContext(null, ALL_OBJECTS, contextId, sharedContextId, apFile, shared, null, "test");
    }

    private static List<File> listFolders(File root) {
        return Stream.of(Objects.requireNonNull(root.listFiles())).filter(File::isDirectory).collect(Collectors.toList());
    }

    private static boolean containsJar(File folder) throws IOException {
        try (Stream<Path> files = Files.walk(folder.toPath())) {
            return files.anyMatch(p -> p.toString().endsWith(".jar"));
        }
    }

    private static List<String> listTemporaryFoldersInWorkingDirectory() {
        return Stream.of(Objects.requireNonNull(new File(".").listFiles()))
            .map(File::getName)
            .filter(n -> n.startsWith("resources_") || n.startsWith("ap_staging_resources_"))
            .sorted()
            .collect(Collectors.toList());
    }
}
