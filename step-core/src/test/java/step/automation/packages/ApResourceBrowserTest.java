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
import step.attachments.FileResolver;
import step.automation.packages.ApResourceBrowser.EntryFilter;
import step.core.filebrowser.DirectoryListing;
import step.core.filebrowser.FileDescriptor;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

public class ApResourceBrowserTest {

    /**
     * The reference builder of the controller mode: a deployed automation package.
     */
    private static final Function<String, String> AP_A = path -> FileResolver.createPathForApResource("apA", path);

    /**
     * The reference builder of the local IDE mode: the relative path is the reference.
     */
    private static final Function<String, String> RELATIVE = Function.identity();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File archiveDirectory;
    private File archiveZip;

    @Before
    public void setUp() throws Exception {
        // Build an exploded automation package directory, then also a zipped version of it, so that every
        // assertion can be run against both flavours of archive.
        archiveDirectory = tmp.newFolder("ap-source");
        writeFile(new File(archiveDirectory, "automation-package.yml"), "name: myAp");
        writeFile(new File(archiveDirectory, "data/pool.csv"), "a,b,c");
        writeFile(new File(archiveDirectory, "k6/mytest/test.js"), "export default function(){}");
        writeFile(new File(archiveDirectory, "k6/mytest/lib/helper.js"), "export const x = 1;");

        archiveZip = new File(tmp.getRoot(), "ap.zip");
        FileHelper.zip(archiveDirectory, archiveZip);
    }

    private static void writeFile(File file, String content) throws Exception {
        Files.createDirectories(file.getParentFile().toPath());
        Files.writeString(file.toPath(), content);
    }

    private static List<String> names(DirectoryListing content) {
        return content.entries().stream().map(FileDescriptor::name).collect(Collectors.toList());
    }

    @Test
    public void listsTheRootOfTheArchive() {
        for (File archive : List.of(archiveZip, archiveDirectory)) {
            DirectoryListing content = ApResourceBrowser.browse(archive, null, AP_A);
            assertEquals("", content.path());
            assertNull(content.parentPath());
            // the root itself has no relative path, hence no reference
            assertNull(content.resourceReference());
            // directories first, then files, each alphabetically
            assertEquals(List.of("data", "k6", "automation-package.yml"), names(content));

            FileDescriptor directory = content.entries().get(0);
            assertTrue(directory.directory());
            assertFalse(directory.regularFile());
            assertNull(directory.size());
            assertEquals("apResource:apA:data", directory.resourceReference());

            FileDescriptor file = content.entries().get(2);
            assertFalse(file.directory());
            assertTrue(file.regularFile());
            assertEquals("automation-package.yml", file.path());
            assertEquals(Long.valueOf("name: myAp".getBytes(StandardCharsets.UTF_8).length), file.size());
            assertEquals("apResource:apA:automation-package.yml", file.resourceReference());

            // an archive carries no such attribute, in either flavour
            assertFalse(file.hidden());
            assertFalse(file.symlink());
        }
    }

    @Test
    public void listsANestedDirectory() {
        for (File archive : List.of(archiveZip, archiveDirectory)) {
            DirectoryListing content = ApResourceBrowser.browse(archive, "k6/mytest", AP_A);
            assertEquals("k6/mytest", content.path());
            assertEquals("k6", content.parentPath());
            assertEquals("apResource:apA:k6/mytest", content.resourceReference());
            assertEquals(List.of("lib", "test.js"), names(content));
            assertEquals("apResource:apA:k6/mytest/test.js", content.entries().get(1).resourceReference());
        }
    }

    /**
     * The local IDE edits the YAML descriptor in place, whose authoring format is a plain relative
     * path: there is no automation package id to build a reference with, so the path is the reference.
     */
    @Test
    public void buildsPlainRelativeReferencesForTheLocalMode() {
        for (File archive : List.of(archiveZip, archiveDirectory)) {
            DirectoryListing content = ApResourceBrowser.browse(archive, "k6/mytest", RELATIVE);
            assertEquals("k6/mytest", content.resourceReference());
            assertEquals("k6/mytest/test.js", content.entries().get(1).resourceReference());
        }
    }

    @Test
    public void opensTheParentDirectoryWhenThePathIsAFile() {
        for (File archive : List.of(archiveZip, archiveDirectory)) {
            DirectoryListing content = ApResourceBrowser.browse(archive, "k6/mytest/test.js", AP_A);
            assertEquals("k6/mytest", content.path());
            assertEquals(List.of("lib", "test.js"), names(content));
        }
    }

    /**
     * A dangling reference must be reported rather than swallowed behind the listing of some surviving
     * ancestor directory.
     */
    @Test
    public void failsOnAnUnknownPath() {
        for (File archive : List.of(archiveZip, archiveDirectory)) {
            try {
                ApResourceBrowser.browse(archive, "k6/mytest/does/not/exist.js", AP_A);
                fail("expected ApResourceNotFoundException");
            } catch (ApResourceNotFoundException expected) {
                assertTrue(expected.getMessage().contains("k6/mytest/does/not/exist.js"));
            }
        }
    }

    @Test
    public void filtersTheListedEntries() {
        for (File archive : List.of(archiveZip, archiveDirectory)) {
            assertEquals(List.of("automation-package.yml"),
                names(ApResourceBrowser.browse(archive, null, AP_A, EntryFilter.FILES_ONLY)));
            assertEquals(List.of("data", "k6"),
                names(ApResourceBrowser.browse(archive, null, AP_A, EntryFilter.DIRECTORIES_ONLY)));
            assertEquals(List.of("data", "k6", "automation-package.yml"),
                names(ApResourceBrowser.browse(archive, null, AP_A, EntryFilter.ALL)));
        }
    }

    @Test
    public void mapsTheFilterQueryParameters() {
        assertEquals(EntryFilter.ALL, EntryFilter.of(false, false));
        assertEquals(EntryFilter.FILES_ONLY, EntryFilter.of(true, false));
        assertEquals(EntryFilter.DIRECTORIES_ONLY, EntryFilter.of(false, true));
        assertThrows(IllegalArgumentException.class, () -> EntryFilter.of(true, true));
    }

    @Test
    public void normalisesTheRequestedPath() {
        for (File archive : List.of(archiveZip, archiveDirectory)) {
            assertEquals("k6/mytest", ApResourceBrowser.browse(archive, "./k6/mytest/", AP_A).path());
            assertEquals("k6/mytest", ApResourceBrowser.browse(archive, "k6\\mytest", AP_A).path());
            assertEquals("", ApResourceBrowser.browse(archive, "/", AP_A).path());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void browseRejectsTraversalEscape() {
        ApResourceBrowser.browse(archiveZip, "../escape", AP_A);
    }

    @Test(expected = ApResourceNotFoundException.class)
    public void browseFailsOnMissingArchive() {
        ApResourceBrowser.browse(new File(tmp.getRoot(), "does-not-exist.zip"), null, AP_A);
    }

    @Test
    public void opensTheContentOfAnEntry() throws Exception {
        for (File archive : List.of(archiveZip, archiveDirectory)) {
            try (ApResourceBrowser.ApResourceStream stream = ApResourceBrowser.openEntry(archive, "data/pool.csv")) {
                assertEquals("pool.csv", stream.getName());
                assertEquals("a,b,c", new String(stream.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                assertEquals(5L, stream.getSize());
            }
        }
    }

    @Test
    public void openEntryFailsOnMissingEntry() {
        for (File archive : List.of(archiveZip, archiveDirectory)) {
            try {
                ApResourceBrowser.openEntry(archive, "data/missing.csv");
                fail("expected ApResourceNotFoundException");
            } catch (ApResourceNotFoundException expected) {
                assertTrue(expected.getMessage().contains("data/missing.csv"));
            }
        }
    }

    @Test
    public void openEntryFailsOnDirectory() {
        for (File archive : List.of(archiveZip, archiveDirectory)) {
            try {
                ApResourceBrowser.openEntry(archive, "k6/mytest");
                fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("is a directory"));
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void openEntryRejectsTraversalEscape() {
        ApResourceBrowser.openEntry(archiveZip, "../escape.txt");
    }

    // ---------------------------------------------------------------------------------------------
    // .apignore - the exploded flavour only. These cannot join the loops above: the fixture zip is
    // built without a filter, whereas the CLI applies the very same .apignore when it builds the
    // archive to deploy, so the two flavours agree in practice but not on this fixture.
    // ---------------------------------------------------------------------------------------------

    /**
     * @return the exploded archive of {@link #setUp()}, with the given {@code .apignore} at its root
     */
    private File archiveDirectoryIgnoring(String... patterns) throws Exception {
        writeFile(new File(archiveDirectory, ".apignore"), String.join("\n", patterns));
        return archiveDirectory;
    }

    @Test
    public void excludesWhatTheApIgnoreExcludes() throws Exception {
        File archive = archiveDirectoryIgnoring("*.csv", "/k6");

        // 'data' survives as an empty directory - only its content matched, as when the CLI zips it
        assertEquals(List.of("data", "automation-package.yml"), names(ApResourceBrowser.browse(archive, null, AP_A)));
        assertEquals(List.of(), names(ApResourceBrowser.browse(archive, "data", AP_A)));
    }

    /**
     * The file declaring the patterns is not part of the package either - the CLI leaves it out of the
     * archive it builds, so the browser must not offer a reference to it.
     */
    @Test
    public void excludesTheApIgnoreFileItself() throws Exception {
        File archive = archiveDirectoryIgnoring("*.log");

        assertEquals(List.of("data", "k6", "automation-package.yml"), names(ApResourceBrowser.browse(archive, null, AP_A)));
    }

    /**
     * A directory rejected by the .apignore is not entered at all, exactly as {@code FileHelper.zip}
     * does not recurse into one when the CLI builds the archive. The nested file matches no pattern of
     * its own, so a per-entry filter would keep it and produce a listing the deployed package would not.
     */
    @Test
    public void excludesTheWholeSubtreeOfAnExcludedDirectory() throws Exception {
        File archive = archiveDirectoryIgnoring("/k6");

        assertEquals(List.of("data", "automation-package.yml"), names(ApResourceBrowser.browse(archive, null, AP_A)));
        try {
            ApResourceBrowser.browse(archive, "k6/mytest/test.js", AP_A);
            fail("expected ApResourceNotFoundException");
        } catch (ApResourceNotFoundException expected) {
            assertTrue(expected.getMessage().contains("k6/mytest/test.js"));
        }
    }

    /**
     * A 404, and one that says why: the file is there, the editor shows it, it is simply not part of
     * the package.
     */
    @Test
    public void openEntryReportsAnExcludedEntryAsIgnored() throws Exception {
        File archive = archiveDirectoryIgnoring("/k6");

        for (String excluded : List.of("k6/mytest/test.js", ".apignore")) {
            try {
                ApResourceBrowser.openEntry(archive, excluded);
                fail("expected ApResourceNotFoundException");
            } catch (ApResourceNotFoundException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains(excluded));
                assertTrue(expected.getMessage(), expected.getMessage().contains(".apignore"));
            }
        }
    }

    @Test
    public void listsEverythingWithoutAnApIgnore() {
        assertEquals(List.of("data", "k6", "automation-package.yml"), names(ApResourceBrowser.browse(archiveDirectory, null, AP_A)));
    }

    /**
     * A zip carries no .apignore of its own to interpret: the CLI already applied it when building the
     * archive, and hiding entries that are genuinely in the package would be wrong.
     */
    @Test
    public void doesNotInterpretAnApIgnoreFoundInAZip() throws Exception {
        archiveDirectoryIgnoring("/k6");
        File zipWithApIgnore = new File(tmp.getRoot(), "with-apignore.zip");
        FileHelper.zip(archiveDirectory, zipWithApIgnore);

        // containment rather than an exact list: where '.apignore' sorts among the file names is the
        // collator's business, and not what this test is about
        assertTrue(names(ApResourceBrowser.browse(zipWithApIgnore, null, AP_A))
            .containsAll(List.of("data", "k6", ".apignore", "automation-package.yml")));
    }

    /**
     * Zip archives are not required to carry explicit directory entries. The directories they only imply
     * must nevertheless be browsable.
     */
    @Test
    public void listsDirectoriesOnlyImpliedByFileEntries() throws Exception {
        File flatZip = new File(tmp.getRoot(), "flat.zip");
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(flatZip))) {
            for (String entryName : List.of("k6/mytest/test.js", "k6/mytest/lib/helper.js")) {
                zip.putNextEntry(new ZipEntry(entryName));
                zip.write("content".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }

        DirectoryListing root = ApResourceBrowser.browse(flatZip, null, AP_A);
        assertEquals(List.of("k6"), names(root));

        DirectoryListing nested = ApResourceBrowser.browse(flatZip, "k6/mytest", AP_A);
        assertEquals(List.of("lib", "test.js"), names(nested));
    }
}
