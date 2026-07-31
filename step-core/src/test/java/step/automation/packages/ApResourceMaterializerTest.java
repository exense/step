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
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class ApResourceMaterializerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final ApResourceMaterializer materializer = new ApResourceMaterializer();

    private File cacheRoot;
    private File archiveZip;
    private File archiveFolder;

    @Before
    public void setUp() throws Exception {
        cacheRoot = tmp.newFolder("AP_cache");

        // Build an exploded automation package folder, then also a zipped version of it.
        archiveFolder = tmp.newFolder("ap-source");
        writeFile(new File(archiveFolder, "scripts/kw.groovy"), "println 'hello'");
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

    private Supplier<AutomationPackageArchive> zipSupplier(AtomicInteger opens) {
        return () -> {
            opens.incrementAndGet();
            return newArchive(archiveZip);
        };
    }

    private static AutomationPackageArchive newArchive(File file) {
        try {
            return new JavaAutomationPackageArchive(file, null, null);
        } catch (AutomationPackageReadingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void materialisesSingleFileFromZip() throws Exception {
        File file = materializer.materialize(cacheRoot, "apA", "scripts/kw.groovy", zipSupplier(new AtomicInteger()));
        assertTrue(file.isFile());
        assertEquals("println 'hello'", Files.readString(file.toPath()));
        assertEquals(new File(new File(cacheRoot, "apA"), "scripts/kw.groovy").getAbsolutePath(), file.getAbsolutePath());
    }

    @Test
    public void secondResolveHitsFastPathAndDoesNotOpenArchive() {
        AtomicInteger opens = new AtomicInteger();
        File first = materializer.materialize(cacheRoot, "apA", "scripts/kw.groovy", zipSupplier(opens));
        File second = materializer.materialize(cacheRoot, "apA", "scripts/kw.groovy", zipSupplier(opens));
        assertEquals(first.getAbsolutePath(), second.getAbsolutePath());
        assertEquals("archive must be opened exactly once", 1, opens.get());
    }

    @Test
    public void normalisesLeadingDotSlash() {
        File a = materializer.materialize(cacheRoot, "apA", "./scripts/kw.groovy", zipSupplier(new AtomicInteger()));
        File b = materializer.materialize(cacheRoot, "apA", "scripts/kw.groovy", zipSupplier(new AtomicInteger()));
        assertEquals(a.getAbsolutePath(), b.getAbsolutePath());
    }

    @Test
    public void missingEntryThrowsNotFound() {
        try {
            materializer.materialize(cacheRoot, "apA", "scripts/does-not-exist.groovy", zipSupplier(new AtomicInteger()));
            fail("expected ApResourceNotFoundException");
        } catch (ApResourceNotFoundException expected) {
            assertTrue(expected.getMessage().contains("does-not-exist.groovy"));
        }
    }

    @Test(expected = RuntimeException.class)
    public void rejectsTraversalEscape() {
        materializer.materialize(cacheRoot, "apA", "../escape.txt", zipSupplier(new AtomicInteger()));
    }

    @Test
    public void materialisesDirectoryTreeFromZip() throws Exception {
        File dir = materializer.materialize(cacheRoot, "apA", "k6/mytest", zipSupplier(new AtomicInteger()));
        assertTrue(dir.isDirectory());
        assertEquals("export default function(){}", Files.readString(new File(dir, "test.js").toPath()));
        assertEquals("export const x = 1;", Files.readString(new File(dir, "lib/helper.js").toPath()));
    }

    @Test
    public void materialisesFromExplodedFolderArchive() throws Exception {
        Supplier<AutomationPackageArchive> folderSupplier = () -> newArchive(archiveFolder);
        File file = materializer.materialize(cacheRoot, "apLocal", "data/pool.csv", folderSupplier);
        assertTrue(file.isFile());
        assertEquals("a,b,c", Files.readString(file.toPath()));
    }

    /**
     * The archive is opened through the percent encoded URLs of a class loader. A space in the name of
     * the archive (a jar downloaded twice by a browser ends up as "... (1).jar") or in the path of an
     * entry must therefore not prevent its materialisation.
     */
    @Test
    public void materialisesFromArchiveAndEntriesWithSpacesInTheirNames() throws Exception {
        writeFile(new File(archiveFolder, "data/my pool.csv"), "x,y,z");
        writeFile(new File(archiveFolder, "my scripts/my kw.groovy"), "println 'hi'");
        File archiveWithSpaces = new File(tmp.getRoot(), "java-automation-package-0.0.0-SNAPSHOT (1).jar");
        FileHelper.zip(archiveFolder, archiveWithSpaces);
        Supplier<AutomationPackageArchive> supplier = () -> newArchive(archiveWithSpaces);

        assertEquals("a,b,c", Files.readString(materializer.materialize(cacheRoot, "apA", "data/pool.csv", supplier).toPath()));
        assertEquals("x,y,z", Files.readString(materializer.materialize(cacheRoot, "apA", "data/my pool.csv", supplier).toPath()));

        File directory = materializer.materialize(cacheRoot, "apA", "my scripts", supplier);
        assertTrue(directory.isDirectory());
        assertEquals("println 'hi'", Files.readString(new File(directory, "my kw.groovy").toPath()));
    }

    @Test
    public void concurrentResolveOfSameEntryIsConsistent() throws Exception {
        int threads = 8;
        AtomicInteger opens = new AtomicInteger();
        Thread[] workers = new Thread[threads];
        File[] results = new File[threads];
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            workers[i] = new Thread(() ->
                    results[idx] = materializer.materialize(cacheRoot, "apA", "scripts/kw.groovy", zipSupplier(opens)));
        }
        for (Thread w : workers) {
            w.start();
        }
        for (Thread w : workers) {
            w.join();
        }
        for (File r : results) {
            assertNotNull(r);
            assertEquals("println 'hello'", Files.readString(r.toPath()));
        }
    }
}
