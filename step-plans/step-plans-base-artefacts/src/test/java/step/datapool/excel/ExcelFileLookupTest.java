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
package step.datapool.excel;

import ch.exense.commons.io.FileHelper;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.AbstractArtefactTest;
import step.attachments.FileResolver;
import step.automation.packages.ApResourceProvider;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.execution.ExecutionContext;
import step.resources.Resource;
import step.resources.ResourceManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * How the workbook of an Excel data source is named: a path of the file system, a reference to resolve, or
 * a plain name looked up among the files of the execution.
 */
public class ExcelFileLookupTest extends AbstractArtefactTest {

    private ExecutionContext context;
    private ExcelFileLookup lookup;
    private File apResourceFile;

    @Before
    public void setUp() throws IOException {
        context = newExecutionContext();
        apResourceFile = new File(FileHelper.createTempFolder(), "materialised.xlsx");
        Files.writeString(apResourceFile.toPath(), "content");
        // the provider of a deployed automation package, materialising the entry it is asked for
        context.setApResourceProvider((ApResourceProvider) (apId, relativePath) -> apResourceFile);
        lookup = new ExcelFileLookup(context);
    }

    /**
     * The archive-relative path of an {@code apResource:} reference holds separators of its own, which
     * must not make the reference look like a path of the file system.
     */
    @Test
    public void testApResourceReferenceIsResolved() {
        assertEquals(apResourceFile, lookup.lookup("apResource:66f1f77bcf86cd799439011:data/my book.xlsx"));
    }

    /**
     * The reference of a Step resource resolves the same way, and did so before {@code apResource:}
     * existed - it holds no separator of its own, so it never looked like a path.
     */
    @Test
    public void testResourceReferenceIsResolved() throws Exception {
        Resource resource = context.getResourceManager().createResource(ResourceManager.RESOURCE_TYPE_DATASOURCE,
            new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)), "my book.xlsx", null, null);

        File workBookFile = lookup.lookup(FileResolver.RESOURCE_PREFIX + resource.getId().toHexString());

        assertEquals("my book.xlsx", workBookFile.getName());
        assertTrue(workBookFile.getAbsolutePath(), workBookFile.exists());
    }

    /**
     * A path is a path, reference prefixes aside - it is used as it is, without a lookup.
     */
    @Test
    public void testFilesystemPathIsTakenAsItIs() {
        assertEquals(new File("/tmp/books/my book.xlsx"), lookup.lookup("/tmp/books/my book.xlsx"));
        assertEquals(new File("C:\\books\\my book.xlsx"), lookup.lookup("C:\\books\\my book.xlsx"));
    }

    /**
     * A plain name is the name of a file of the execution, bound as a {@code $file:} variable - by the
     * artefact that made it available.
     */
    @Test
    public void testPlainNameIsLookedUpAmongTheFilesOfTheExecution() throws IOException {
        File workbook = new File(FileHelper.createTempFolder(), "my book.xlsx");
        Files.writeString(workbook.toPath(), "content");
        context.getVariablesManager().putVariable(context.getReport(),
            ArtefactHandler.FILE_VARIABLE_PREFIX + "my book.xlsx", workbook);

        assertEquals(workbook, lookup.lookup("my book.xlsx"));
    }

    @Test
    public void testUnknownNameIsReported() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> lookup.lookup("no such book.xlsx"));

        assertEquals("The workbook 'no such book.xlsx' couldn't be found.", exception.getMessage());
    }
}
