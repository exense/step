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
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
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

        PackageExecutionContext context1 = createContext(contextId, false);
        PackageExecutionContext context2 = createContext(contextId, false);

        List<File> folders = listFolders(isolatedRoot);
        assertEquals(2, folders.size());
        folders.forEach(f -> assertTrue(f.getName().startsWith("resources_" + contextId + "_")));
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
    public void sharedContextIsOnlyUsedByAllowedExecutions() throws IOException {
        // store the AP as the executor does, to support the restore of the re-executions
        ObjectId contextId = new ObjectId();
        AutomationPackageFile storedApFile;
        try (InputStream is = new FileInputStream(apFile.getFile())) {
            storedApFile = repository.getApFileForExecution(is, apFile.getFile().getName(), null, contextId, NO_ENRICHMENT,
                ALL_OBJECTS, "test", ResourceManager.RESOURCE_TYPE_ISOLATED_AP);
        }
        IsolatedPackageExecutionContext sharedContext = repository.createIsolatedPackageExecutionContext(NO_ENRICHMENT, ALL_OBJECTS,
            contextId.toString(), storedApFile, true, null, "test");
        String apName = sharedContext.getAutomationPackage().getAttribute(AbstractOrganizableObject.NAME);
        repository.setApNameForResource(storedApFile.getResource(), apName);

        String allowedExecutionId = new ObjectId().toString();
        sharedContext.allowExecution(allowedExecutionId);
        Map<String, String> repositoryParameters = Map.of(REPOSITORY_PARAM_CONTEXTID, contextId.toString(), AP_NAME, apName);

        assertSame(sharedContext, repository.getOrRestorePackageExecutionContext(allowedExecutionId, repositoryParameters, null, ALL_OBJECTS, null));

        // a re-execution with the same context id gets its own context
        PackageExecutionContext reExecutionContext = repository.getOrRestorePackageExecutionContext(new ObjectId().toString(), repositoryParameters, null, ALL_OBJECTS, null);
        assertNotSame(sharedContext, reExecutionContext);
        assertFalse(reExecutionContext.isShared());
        PackageExecutionContext otherContext = repository.getOrRestorePackageExecutionContext(null, repositoryParameters, null, ALL_OBJECTS, null);
        assertNotSame(sharedContext, otherContext);
        assertEquals(3, listFolders(isolatedRoot).size());

        // the end of the shared context doesn't affect the re-execution
        sharedContext.close();
        assertNull(repository.sharedPackageExecutionContexts.get(contextId.toString()));
        otherContext.close();
        List<File> remainingFolders = listFolders(isolatedRoot);
        assertEquals(1, remainingFolders.size());
        assertTrue(containsJar(remainingFolders.get(0)));

        reExecutionContext.close();
        assertEquals(0, listFolders(isolatedRoot).size());
    }

    private IsolatedPackageExecutionContext createContext(String contextId, boolean shared) {
        return repository.createIsolatedPackageExecutionContext(null, ALL_OBJECTS, contextId, apFile, shared, null, "test");
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
