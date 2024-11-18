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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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
        private final File containerDirectory;

        public ExtractedDirectory(File containerDirectory, File directory, boolean deleteOnClose) {
            this.containerDirectory = containerDirectory;
            this.deleteOnClose = deleteOnClose;
            this.directory = directory;
        }

        @Override
        public void close() {
            if (deleteOnClose) {
                FileHelper.deleteFolder(containerDirectory);
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
            return new ExtractedDirectory(null, folderToZip, false);
        } else if (protocol.equals(JAR)) {
            JarResourcePath jarResourcePath = new JarResourcePath(resourceUrl);
            File tempFolder = FileHelper.createTempFolder();
            Path extractedDirectory = tempFolder.toPath().resolve(jarResourcePath.pathInJar);
            try (FileSystem fileSystem = FileSystems.newFileSystem(resourceUrl.toURI(), Collections.emptyMap())) {
                Path resourcePath = fileSystem.getPath("/" + jarResourcePath.pathInJar);
                try (Stream<Path> walk = Files.walk(resourcePath)) {
                    walk.forEach(path -> {
                        try {
                            Files.copy(path, extractedDirectory.resolve(resourcePath.relativize(path).toString()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
            return new ExtractedDirectory(tempFolder, extractedDirectory.toFile(), true);
        } else {
            throw unsupportedProtocol(protocol);
        }
    }

    public static List<URL> listDirectory(URL resourceUrl) throws IOException, URISyntaxException {
        if(ClassLoaderResourceFilesystem.isDirectory(resourceUrl)) {
            String protocol = resourceUrl.getProtocol();
            if (protocol.equals(FILE)) {
                File directory = new File(resourceUrl.getPath());
                return Arrays.stream(directory.listFiles()).map(f -> toURL(f.toURI())).collect(Collectors.toList());
            } else if (protocol.equals(JAR)) {
                ClassLoaderResourceFilesystem.JarResourcePath jarResourcePath = new ClassLoaderResourceFilesystem.JarResourcePath(resourceUrl);
                try (FileSystem fileSystem = FileSystems.newFileSystem(resourceUrl.toURI(), Collections.emptyMap())) {
                    Path resourcePath = fileSystem.getPath("/" + jarResourcePath.pathInJar);
                    return Files.list(resourcePath).map(path -> toURL(path.toUri())).collect(Collectors.toList());
                }
            } else {
                throw unsupportedProtocol(protocol);
            }
        } else {
            throw new RuntimeException("The provided resource " + resourceUrl + " is not a directory");
        }
    }

    private static URL toURL(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static class JarResourcePath {

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
