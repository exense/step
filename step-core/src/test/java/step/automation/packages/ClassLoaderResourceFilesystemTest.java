/*
 * Copyright (C) 2024, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

package step.automation.packages;

import ch.exense.commons.io.FileHelper;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

public class ClassLoaderResourceFilesystemTest {

    @Test
    public void testJarProtocol() throws Exception {
        URL zip = this.getClass().getClassLoader().getResource("folder.zip");
        URL folder = new URLClassLoader(new URL[]{zip}).getResource("folder");
        URL subfolder = new URLClassLoader(new URL[]{zip}).getResource("folder/subfolder");
        URL textResource = new URLClassLoader(new URL[]{zip}).getResource("folder/TestResource.txt");
        assertTrue(ClassLoaderResourceFilesystem.isDirectory(folder));
        assertTrue(ClassLoaderResourceFilesystem.isDirectory(subfolder));
        assertFalse(ClassLoaderResourceFilesystem.isDirectory(textResource));

        File directory;
        try (ClassLoaderResourceFilesystem.ExtractedDirectory extractedDirectory = ClassLoaderResourceFilesystem.extractDirectory(folder)) {
            directory = extractedDirectory.directory;
            assertTrue(directory.isDirectory());
            assertEquals("folder", directory.getName());
        }
        // Assert that the temporary directory has been deleted
        assertFalse(directory.exists());
    }

    @Test
    public void testFileProtocol() throws IOException, URISyntaxException {
        Path target = FileHelper.createTempFolder().toPath();
        FileHelper.unzip(this.getClass().getClassLoader().getResource("folder.zip").openStream(), target.toFile());
        URL folder = target.resolve("folder").toUri().toURL();
        URL subfolder = target.resolve("folder/subfolder").toUri().toURL();
        URL textResource = target.resolve("folder/TestResource.txt").toUri().toURL();
        assertTrue(ClassLoaderResourceFilesystem.isDirectory(folder));
        assertTrue(ClassLoaderResourceFilesystem.isDirectory(subfolder));
        assertFalse(ClassLoaderResourceFilesystem.isDirectory(textResource));

        File directory;
        try (ClassLoaderResourceFilesystem.ExtractedDirectory extractedDirectory = ClassLoaderResourceFilesystem.extractDirectory(folder)) {
            directory = extractedDirectory.directory;
            assertEquals(new File(folder.getFile()), directory);
        }
        // Assert that the directory still exists
        assertTrue(directory.exists());
    }

    /**
     * The URLs returned by a class loader are percent encoded. Both the path of the archive itself
     * (a jar downloaded twice by a browser typically ends up as "my archive (1).jar") and the path of
     * the entry within it must be decoded before being used as a filesystem path, respectively as a
     * zip entry name.
     */
    @Test
    public void testJarProtocolWithSpacesInPaths() throws Exception {
        File zip = new File(FileHelper.createTempFolder(), "my archive (1).zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("my folder/"));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("my folder/my file.txt"));
            out.write("content".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        URL zipUrl = zip.toURI().toURL();
        assertTrue("the url of the archive must be encoded for this test to be meaningful", zipUrl.toString().contains("%20"));

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{zipUrl})) {
            URL folder = classLoader.getResource("my folder");
            URL textResource = classLoader.getResource("my folder/my file.txt");
            assertTrue(ClassLoaderResourceFilesystem.isDirectory(folder));
            assertFalse(ClassLoaderResourceFilesystem.isDirectory(textResource));

            try (ClassLoaderResourceFilesystem.ExtractedDirectory extractedDirectory = ClassLoaderResourceFilesystem.extractDirectory(folder)) {
                assertEquals("my folder", extractedDirectory.directory.getName());
                assertEquals("content", Files.readString(new File(extractedDirectory.directory, "my file.txt").toPath()));
            }
        }
    }

    @Test
    public void testFileProtocolWithSpacesInPaths() throws Exception {
        File folder = new File(FileHelper.createTempFolder(), "my folder");
        assertTrue(new File(folder, "sub folder").mkdirs());
        URL folderUrl = folder.toURI().toURL();
        assertTrue("the url of the folder must be encoded for this test to be meaningful", folderUrl.toString().contains("%20"));

        assertTrue(ClassLoaderResourceFilesystem.isDirectory(folderUrl));
        assertEquals(List.of(new File(folder, "sub folder").toURI().toURL()), ClassLoaderResourceFilesystem.listDirectory(folderUrl));
        try (ClassLoaderResourceFilesystem.ExtractedDirectory extractedDirectory = ClassLoaderResourceFilesystem.extractDirectory(folderUrl)) {
            assertEquals(folder, extractedDirectory.directory);
        }
    }

    @Test
    public void testUnsupportedProtocol() {
        assertThrows(RuntimeException.class, () -> ClassLoaderResourceFilesystem.extractDirectory(new URL("http", "myHost", "myFile")));
        assertThrows(RuntimeException.class, () -> ClassLoaderResourceFilesystem.isDirectory(new URL("http", "myHost", "myFile")));
    }
}
