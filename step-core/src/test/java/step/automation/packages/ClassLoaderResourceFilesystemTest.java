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
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

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

    @Test
    public void testUnsupportedProtocol() {
        assertThrows(RuntimeException.class, () -> ClassLoaderResourceFilesystem.extractDirectory(new URL("http", "myHost", "myFile")));
        assertThrows(RuntimeException.class, () -> ClassLoaderResourceFilesystem.isDirectory(new URL("http", "myHost", "myFile")));
    }
}