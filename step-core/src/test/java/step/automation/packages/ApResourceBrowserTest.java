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

import ch.exense.commons.io.FileHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.attachments.ApResourceNotFoundException;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

public class ApResourceBrowserTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File archiveFolder;
    private File archiveZip;

    @Before
    public void setUp() throws Exception {
        // Build an exploded automation package folder, then also a zipped version of it, so that every
        // assertion can be run against both flavours of archive.
        archiveFolder = tmp.newFolder("ap-source");
        writeFile(new File(archiveFolder, "automation-package.yml"), "name: myAp");
        writeFile(new File(archiveFolder, "data/pool.csv"), "a,b,c");
        writeFile(new File(archiveFolder, "k6/mytest/test.js"), "export default function(){}");
        writeFile(new File(archiveFolder, "k6/mytest/lib/helper.js"), "export const x = 1;");

        archiveZip = new File(tmp.getRoot(), "ap.zip");
        FileHelper.zip(archiveFolder, archiveZip);
    }

    private static void writeFile(File file, String content) throws Exception {
        Files.createDirectories(file.getParentFile().toPath());
        Files.writeString(file.toPath(), content);
    }

    private static List<String> names(ApResourceFolderContent content) {
        return content.entries().stream().map(ApResourceEntry::name).collect(Collectors.toList());
    }

    @Test
    public void listsTheRootOfTheArchive() {
        for (File archive : List.of(archiveZip, archiveFolder)) {
            ApResourceFolderContent content = ApResourceBrowser.browse("apA", archive, null);
            assertEquals("", content.path());
            assertNull(content.parentPath());
            // directories first, then files, each alphabetically
            assertEquals(List.of("data", "k6", "automation-package.yml"), names(content));

            ApResourceEntry directory = content.entries().get(0);
            assertTrue(directory.directory());
            assertNull(directory.size());
            assertEquals("apResource:apA:data", directory.reference());

            ApResourceEntry file = content.entries().get(2);
            assertFalse(file.directory());
            assertEquals("automation-package.yml", file.path());
            assertEquals(Long.valueOf("name: myAp".getBytes(StandardCharsets.UTF_8).length), file.size());
            assertEquals("apResource:apA:automation-package.yml", file.reference());
        }
    }

    @Test
    public void listsANestedFolder() {
        for (File archive : List.of(archiveZip, archiveFolder)) {
            ApResourceFolderContent content = ApResourceBrowser.browse("apA", archive, "k6/mytest");
            assertEquals("k6/mytest", content.path());
            assertEquals("k6", content.parentPath());
            assertEquals(List.of("lib", "test.js"), names(content));
            assertEquals("apResource:apA:k6/mytest/test.js", content.entries().get(1).reference());
        }
    }

    @Test
    public void opensTheParentFolderWhenThePathIsAFile() {
        for (File archive : List.of(archiveZip, archiveFolder)) {
            ApResourceFolderContent content = ApResourceBrowser.browse("apA", archive, "k6/mytest/test.js");
            assertEquals("k6/mytest", content.path());
            assertEquals(List.of("lib", "test.js"), names(content));
        }
    }

    @Test
    public void fallsBackToTheClosestExistingFolderForAnUnknownPath() {
        for (File archive : List.of(archiveZip, archiveFolder)) {
            ApResourceFolderContent content = ApResourceBrowser.browse("apA", archive, "k6/mytest/does/not/exist.js");
            assertEquals("k6/mytest", content.path());

            // nothing of the path exists: fall back to the root
            assertEquals("", ApResourceBrowser.browse("apA", archive, "nowhere/at/all").path());
        }
    }

    @Test
    public void normalisesTheRequestedPath() {
        for (File archive : List.of(archiveZip, archiveFolder)) {
            assertEquals("k6/mytest", ApResourceBrowser.browse("apA", archive, "./k6/mytest/").path());
            assertEquals("k6/mytest", ApResourceBrowser.browse("apA", archive, "k6\\mytest").path());
            assertEquals("", ApResourceBrowser.browse("apA", archive, "/").path());
        }
    }

    @Test(expected = RuntimeException.class)
    public void browseRejectsTraversalEscape() {
        ApResourceBrowser.browse("apA", archiveZip, "../escape");
    }

    @Test(expected = ApResourceNotFoundException.class)
    public void browseFailsOnMissingArchive() {
        ApResourceBrowser.browse("apA", new File(tmp.getRoot(), "does-not-exist.zip"), null);
    }

    @Test
    public void opensTheContentOfAnEntry() throws Exception {
        for (File archive : List.of(archiveZip, archiveFolder)) {
            try (ApResourceBrowser.ApResourceStream stream = ApResourceBrowser.openEntry("apA", archive, "data/pool.csv")) {
                assertEquals("pool.csv", stream.getName());
                assertEquals("a,b,c", new String(stream.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                assertEquals(5L, stream.getSize());
            }
        }
    }

    @Test
    public void openEntryFailsOnMissingEntry() {
        for (File archive : List.of(archiveZip, archiveFolder)) {
            try {
                ApResourceBrowser.openEntry("apA", archive, "data/missing.csv");
                fail("expected ApResourceNotFoundException");
            } catch (ApResourceNotFoundException expected) {
                assertTrue(expected.getMessage().contains("data/missing.csv"));
            }
        }
    }

    @Test
    public void openEntryFailsOnDirectory() {
        for (File archive : List.of(archiveZip, archiveFolder)) {
            try {
                ApResourceBrowser.openEntry("apA", archive, "k6/mytest");
                fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("is a directory"));
            }
        }
    }

    @Test(expected = RuntimeException.class)
    public void openEntryRejectsTraversalEscape() {
        ApResourceBrowser.openEntry("apA", archiveZip, "../escape.txt");
    }

    /**
     * Zip archives are not required to carry explicit directory entries. The folders they only imply
     * must nevertheless be browsable.
     */
    @Test
    public void listsFoldersOnlyImpliedByFileEntries() throws Exception {
        File flatZip = new File(tmp.getRoot(), "flat.zip");
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(flatZip))) {
            for (String entryName : List.of("k6/mytest/test.js", "k6/mytest/lib/helper.js")) {
                zip.putNextEntry(new ZipEntry(entryName));
                zip.write("content".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }

        ApResourceFolderContent root = ApResourceBrowser.browse("apA", flatZip, null);
        assertEquals(List.of("k6"), names(root));

        ApResourceFolderContent nested = ApResourceBrowser.browse("apA", flatZip, "k6/mytest");
        assertEquals(List.of("lib", "test.js"), names(nested));
    }
}
