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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
            return toFile(resourceUrl).isDirectory();
        } else if (protocol.equals(JAR)) {
            JarResourcePath jarResourcePath = new JarResourcePath(resourceUrl);
            try (ZipFile zip = new ZipFile(jarResourcePath.jarFile)) {
                ZipEntry entry = zip.getEntry(jarResourcePath.pathInJar);
                boolean isDirectory = entry.isDirectory();
                if (!isDirectory) {
                    try (InputStream input = zip.getInputStream(entry)) {
                        isDirectory = input == null;
                    }
                }
                return isDirectory;
            }
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
            File folderToZip = toFile(resourceUrl);
            return new ExtractedDirectory(null, folderToZip, false);
        } else if (protocol.equals(JAR)) {
            JarResourcePath jarResourcePath = new JarResourcePath(resourceUrl);
            File tempFolder = FileHelper.createTempFolder();
            Path extractedDirectory = tempFolder.toPath().resolve(jarResourcePath.pathInJar);
            extractDirectory(resourceUrl, extractedDirectory);
            return new ExtractedDirectory(tempFolder, extractedDirectory.toFile(), true);
        } else {
            throw unsupportedProtocol(protocol);
        }
    }

    /**
     * Extracts the content of the directory denoted by {@code resourceUrl} into {@code destination},
     * which is created if needed. This is the variant to use when the caller already knows where the
     * content has to end up: {@link #extractDirectory(URL)} extracts a jar entry into a temporary
     * directory of its own, and copying that to the real destination afterwards walks and writes the
     * whole tree a second time.
     * <p>
     * Note that a {@code file:} resource is copied rather than moved: {@link #extractDirectory(URL)}
     * hands out the original directory in that case, which must be left where it is.
     *
     * @param resourceUrl the url of a resource. It supports jar and file protocols. A valid URL for jar
     *                    looks like "jar:file:/path/to/myjar.jar!/folder"
     * @param destination the directory to extract the content into
     */
    public static void extractDirectory(URL resourceUrl, Path destination) throws IOException, URISyntaxException {
        String protocol = resourceUrl.getProtocol();

        if (protocol.equals(FILE)) {
            copyTree(toFile(resourceUrl).toPath(), destination);
        } else if (protocol.equals(JAR)) {
            JarResourcePath jarResourcePath = new JarResourcePath(resourceUrl);
            try (FileSystem fileSystem = FileSystems.newFileSystem(resourceUrl.toURI(), Collections.emptyMap())) {
                copyTree(fileSystem.getPath("/" + jarResourcePath.pathInJar), destination);
            }
        } else {
            throw unsupportedProtocol(protocol);
        }
    }

    /**
     * Recursively copies a directory tree. {@code source} and {@code destination} may belong to
     * different {@link FileSystem}s - a zip one and the default one when extracting a jar entry - which
     * is why the relative path is rebuilt from its string form rather than resolved directly.
     * <p>
     * Domain-free filesystem utility - candidate to be promoted to {@code ch.exense.commons.io.FileHelper}
     * in exense-commons, which has no {@link Path} based recursive copy today.
     */
    private static void copyTree(Path source, Path destination) throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            walk.forEach(path -> {
                try {
                    Path target = destination.resolve(source.relativize(path).toString());
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public static List<URL> listDirectory(URL resourceUrl) throws IOException, URISyntaxException {
        if (ClassLoaderResourceFilesystem.isDirectory(resourceUrl)) {
            String protocol = resourceUrl.getProtocol();
            if (protocol.equals(FILE)) {
                File directory = toFile(resourceUrl);
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

    /**
     * Converts a {@code file:} URL into a {@link File}. The path of a URL is percent encoded (a space is
     * {@code %20}, and a class loader does return such URLs), and can therefore not be used as a
     * filesystem path as-is.
     */
    private static File toFile(URL fileUrl) {
        try {
            return new File(fileUrl.toURI());
        } catch (URISyntaxException | IllegalArgumentException e) {
            // Not every URL is a valid URI: one built by hand may contain characters that a URI requires
            // to be escaped. Such a URL isn't encoded, so its path can be used as it is.
            return new File(fileUrl.getPath());
        }
    }

    /**
     * Percent-decodes the path of a URL, for instance the path of an entry within a jar.
     */
    public static String decodePath(String encodedPath) {
        try {
            // URLDecoder implements the form encoding, in which a '+' stands for a space. In a URL path a
            // '+' is a legal literal character, so protect the ones of the resource name before decoding.
            return URLDecoder.decode(encodedPath.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Malformed escape sequence: the path isn't encoded, use it as it is.
            return encodedPath;
        }
    }

    public static class JarResourcePath {

        public final String pathInJar;
        public final String jarFile;

        public JarResourcePath(URL url) throws MalformedURLException {
            // The file of a jar URL looks like "file:/path/to/my%20jar.jar!/my%20folder/my%20file.txt".
            // Both parts are percent encoded and have to be decoded before being used as a filesystem
            // path, respectively as a zip entry name.
            String urlFile = url.getFile();
            int bangIndex = urlFile.indexOf('!');
            pathInJar = decodePath(urlFile.substring(bangIndex + 2));
            jarFile = toFile(new URL(urlFile.substring(0, bangIndex))).getPath();
        }
    }
}
