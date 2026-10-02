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
package step.plugins.java;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Covers the two layouts an application providing the script engines can be started from: the jars of a class path,
 * and the jars kept inside an executable jar. The libraries are copied as they are in both cases - the agent has to
 * receive the very jars this application runs on, not a reconstruction of them.
 */
public class ScriptEngineLibrariesTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    /**
     * A development run, or any application whose dependencies are plain jars on the class path.
     */
    @Test
    public void copiesALibraryOfTheClassPath() throws Exception {
        Path library = library(folder.getRoot().toPath().resolve("groovy-5.0.4.jar"), "groovy content");
        Path target = folder.newFolder("libraries").toPath();

        ScriptEngineLibraries.copyLibrary(library.toUri().toURL(), target);

        Assert.assertEquals("groovy content", contentOf(target.resolve("groovy-5.0.4.jar")));
    }

    /**
     * An executable jar keeping its dependencies as nested jars, the way Spring Boot packages them. The nested jar has
     * no file of its own, which is exactly what this covers: it is read through the "nested" file system of the
     * loader the application is packaged with.
     */
    @Test
    public void copiesALibraryNestedInAnExecutableJar() throws Exception {
        Path library = jar(folder.getRoot().toPath().resolve("nested/groovy-5.0.4.jar"), "nested groovy content");
        Path application = executableJar(folder.getRoot().toPath().resolve("step.jar"),
            "BOOT-INF/lib/groovy-5.0.4.jar", library);
        Path target = folder.newFolder("libraries").toPath();

        ScriptEngineLibraries.copyLibrary(nestedUrl(application, "BOOT-INF/lib/groovy-5.0.4.jar"), target);

        // The very jar the application runs on, under its own name rather than the name of the application it was
        // nested in
        Path copy = target.resolve("groovy-5.0.4.jar");
        Assert.assertTrue(copy + " was not written", Files.isRegularFile(copy));
        Assert.assertArrayEquals(Files.readAllBytes(library), Files.readAllBytes(copy));
    }

    /**
     * A nested library which is not where the location says it is. Reported rather than left to fail later, when the
     * agent has already been started and the keyword fails with "unable to find script engine".
     */
    @Test
    public void reportsANestedLibraryWhichIsNotThere() throws Exception {
        Path library = jar(folder.getRoot().toPath().resolve("nested/groovy-5.0.4.jar"), "nested groovy content");
        Path application = executableJar(folder.getRoot().toPath().resolve("step.jar"),
            "BOOT-INF/lib/groovy-5.0.4.jar", library);
        Path target = folder.newFolder("libraries").toPath();

        URL missing = nestedUrl(application, "BOOT-INF/lib/nashorn-core-15.4.jar");
        IOException exception = Assert.assertThrows(IOException.class,
            () -> ScriptEngineLibraries.copyLibrary(missing, target));
        Assert.assertTrue(exception.getMessage(), exception.getMessage().contains("nashorn-core-15.4.jar"));
    }

    /**
     * The real thing, on the Groovy this module runs on: the libraries are the jars of the class path, copied whole.
     */
    @Test
    public void extractsTheEngineItRunsOn() throws Exception {
        Path directory = ScriptEngineLibraries.resolve(ScriptEngineLibraries.GROOVY, folder.getRoot().toPath().resolve("groovy"));

        Assert.assertNotNull("Groovy is on the class path of this test", directory);
        Assert.assertTrue("Should hold the groovy jars: " + names(directory),
            names(directory).stream().anyMatch(name -> name.startsWith("groovy-") && name.endsWith(".jar")));
    }

    /**
     * The libraries of a previous run are reused, but only when they are the ones this application runs on: the
     * directory is named after the version, which does not change between two builds of the same one. Without this,
     * a fix to what is extracted would never reach the agents of a developer machine.
     */
    @Test
    public void replacesLibrariesLeftByAnotherExtraction() throws Exception {
        Path target = folder.getRoot().toPath().resolve("groovy");
        Path directory = ScriptEngineLibraries.resolve(ScriptEngineLibraries.GROOVY, target);
        List<String> extracted = names(directory);
        // What a previous version of this class left behind: one jar it had built itself
        for (String name : extracted) {
            Files.delete(directory.resolve(name));
        }
        library(directory.resolve("groovy-libraries.jar"), "what a previous version extracted");

        Assert.assertEquals(directory, ScriptEngineLibraries.resolve(ScriptEngineLibraries.GROOVY, target));
        Assert.assertEquals(extracted, names(directory));
    }

    @Test
    public void reportsALocationItCannotRead() throws Exception {
        Path target = folder.newFolder("libraries").toPath();

        IOException exception = Assert.assertThrows(IOException.class,
            () -> ScriptEngineLibraries.copyLibrary(new URL("http://a.host/groovy.jar"), target));
        Assert.assertTrue(exception.getMessage(), exception.getMessage().contains("groovy.jar"));
    }

    /**
     * The loader names a nested library by the entry it is stored under, without encoding it, so a library whose name
     * holds a character a URI cannot carry gives a location which cannot be read apart. Reported as every other
     * unreadable location is, rather than as the exception of whatever parses it.
     */
    @Test
    public void reportsANestedLocationItCannotReadApart() throws Exception {
        Path target = folder.newFolder("libraries").toPath();
        URL illegal = nestedUrl(folder.getRoot().toPath().resolve("step.jar"), "BOOT-INF/lib/my lib.jar");

        IOException exception = Assert.assertThrows(IOException.class,
            () -> ScriptEngineLibraries.copyLibrary(illegal, target));
        Assert.assertTrue(exception.getMessage(), exception.getMessage().contains("my lib.jar"));
    }

    /**
     * @return the location a class of a nested library is reported at, as the class loader of an executable jar builds
     * it: {@code jar:nested:/path/step.jar/!BOOT-INF/lib/library.jar!/}. Such a URL is built with a handler of its
     * own: the handler of the JVM rejects it, as the "nested" protocol it points at is one the loader of the
     * application registers, and a test runs without that loader.
     */
    private static URL nestedUrl(Path application, String entryName) throws IOException {
        return new URL(null, "jar:nested:" + application.toUri().getRawPath() + "/!" + entryName + "!/",
            new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) {
                    throw new UnsupportedOperationException("The content of " + url + " is not read through the URL");
                }
            });
    }

    private static Path library(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        try (OutputStream out = Files.newOutputStream(path)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return path;
    }

    private static Path jar(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        try (ZipOutputStream jar = new ZipOutputStream(Files.newOutputStream(path))) {
            jar.putNextEntry(new ZipEntry("content.txt"));
            jar.write(content.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return path;
    }

    /**
     * An application packaged as an executable jar holds its libraries as nested jars, stored uncompressed so that
     * they can be read where they lie.
     */
    private static Path executableJar(Path path, String entryName, Path nestedLibrary) throws IOException {
        Files.createDirectories(path.getParent());
        byte[] content = Files.readAllBytes(nestedLibrary);
        CRC32 crc = new CRC32();
        crc.update(content);
        ZipEntry entry = new ZipEntry(entryName);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(content.length);
        entry.setCrc(crc.getValue());
        try (ZipOutputStream jar = new ZipOutputStream(Files.newOutputStream(path))) {
            jar.putNextEntry(entry);
            jar.write(content);
            jar.closeEntry();
        }
        return path;
    }

    private static List<String> names(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return files.map(path -> path.getFileName().toString()).sorted().collect(Collectors.toList());
        }
    }

    private static String contentOf(Path file) throws IOException {
        Assert.assertTrue(file + " was not written", Files.isRegularFile(file));
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
