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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassLoaderResourceFilesystem {

    public static final String FILE = "file";
    public static final String JAR = "jar";

    /**
     * @param resourceUrl the url of a resource. It supports jar and file protocols. A valid URL for jar looks like "jar:file:/path/to/myjar.jar!/folder"
     * @return true if the resource referenced by the provided url is a directory
     * @throws IOException
     */
    public static boolean isDirectory(URL resourceUrl) throws IOException {
        String protocol = resourceUrl.getProtocol();
        if (protocol.equals(FILE)) {
            return new File(resourceUrl.getFile()).isDirectory();
        } else if (protocol.equals(JAR)) {
            JarResourcePath jarResourcePath = new JarResourcePath(resourceUrl);
            ZipFile zip = new ZipFile(jarResourcePath.jarFile);
            ZipEntry entry = zip.getEntry(jarResourcePath.pathInJar);
            boolean isDirectory = entry.isDirectory();
            if (!isDirectory) {
                InputStream input = zip.getInputStream(entry);
                isDirectory = input == null;
                if (input != null) input.close();
            }
            return isDirectory;
        } else {
            throw unsupportedProtocol(protocol);
        }
    }

    private static RuntimeException unsupportedProtocol(String protocol) {
        return new RuntimeException("Unsupported protocol: " + protocol);
    }

    public static class ExtractedDirectory implements AutoCloseable {

        private final boolean deleteOnClose;
        public final File directory;

        public ExtractedDirectory(File directory, boolean deleteOnClose) {
            this.deleteOnClose = deleteOnClose;
            this.directory = directory;
        }

        @Override
        public void close() {
            if (deleteOnClose) {
                FileHelper.deleteFolder(directory);
            }
        }
    }

    /**
     * @param resourceUrl the url of a resource. It supports jar and file protocols. A valid URL for jar looks like "jar:file:/path/to/myjar.jar!/folder"
     * @return a {@link ExtractedDirectory} that points to the extracted directory. {@link ExtractedDirectory} is {@link AutoCloseable} and removes temporary files upon close()
     * @throws IOException
     * @throws URISyntaxException
     */
    public static ExtractedDirectory extractDirectory(URL resourceUrl) throws IOException, URISyntaxException {
        String protocol = resourceUrl.getProtocol();

        if (protocol.equals(FILE)) {
            File folderToZip = new File(resourceUrl.getPath());
            return new ExtractedDirectory(folderToZip, false);
        } else if (protocol.equals(JAR)) {
            JarResourcePath jarResourcePath = new JarResourcePath(resourceUrl);
            File tempFolder = FileHelper.createTempFolder();
            try (FileSystem fileSystem = FileSystems.newFileSystem(resourceUrl.toURI(), Collections.emptyMap())) {
                Path resourcePath = fileSystem.getPath("/" + jarResourcePath.pathInJar);
                try (Stream<Path> walk = Files.walk(resourcePath)) {
                    walk.forEach(path -> {
                        try {
                            Files.copy(path, tempFolder.toPath().resolve(jarResourcePath.pathInJar).resolve(resourcePath.relativize(path).toString()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
            return new ExtractedDirectory(tempFolder, true);
        } else {
            throw unsupportedProtocol(protocol);
        }
    }

    private static class JarResourcePath {

        public final String pathInJar;
        public final String jarFile;

        public JarResourcePath(URL url) throws MalformedURLException {
            String urlFile = url.getFile();
            int bangIndex = urlFile.indexOf('!');
            pathInJar = urlFile.substring(bangIndex + 2);
            jarFile = new URL(urlFile.substring(0, bangIndex)).getFile();
        }
    }
}
