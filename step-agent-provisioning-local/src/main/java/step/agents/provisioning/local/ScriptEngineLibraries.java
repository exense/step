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
package step.agents.provisioning.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Provides the script engine libraries a Groovy or JavaScript keyword needs, so that they can be sent to the agent
 * executing it.
 * <p>
 * These keywords run through {@code javax.script}, and their engine has to be on the class path of the <b>agent</b>.
 * A Step controller ships the engines in {@code ext/groovy} and {@code ext/javascript} and points
 * {@code plugins.<language>.libs} at them, which is what makes them travel to the agent as the keyword's plugin
 * libraries. An application starting agents of its own, the CLI in particular, has no such directory, and this class
 * builds the equivalent. A configured {@code plugins.<language>.libs} always wins, so a caller which does ship the
 * engines keeps shipping its own.
 * <p>
 * The libraries are not shipped a second time: the engines are already on the class path of whoever runs this, because
 * step-core depends on {@code groovy-all} and {@code nashorn-core} for its own expression handling. Their jars are
 * therefore copied out of it, as they are, once per version.
 * <p>
 * <b>The jars are copied whole, never rebuilt.</b> An engine is spread over several libraries, each carrying its own
 * service declarations and, for Groovy, its own extension module descriptors. Selecting classes out of them - which is
 * what a merged fat jar leaves as the only option - means naming the packages of an engine and re-declaring what those
 * descriptors say, and silently loses a feature whenever an engine reorganizes itself. Copying the libraries as they
 * are gives the agent exactly what this application runs on. It is also why a single merged jar is rejected outright
 * rather than filtered: see {@link #locationOf}.
 */
public class ScriptEngineLibraries {

    private static final Logger logger = LoggerFactory.getLogger(ScriptEngineLibraries.class);

    /**
     * An engine, as found on the class path of the running application.
     *
     * @param language      the language as Step names it, i.e. the {@code plugins.<language>.libs} property
     * @param factoryClass  the {@code ScriptEngineFactory}, by which the presence of the engine is detected
     * @param markerClasses one class per library the engine is spread over, used to locate those libraries. Absent
     *                      ones are skipped, so this list can name modules which are not always there.
     */
    public record ScriptEngine(String language, String factoryClass, List<String> markerClasses) {
    }

    public static final ScriptEngine GROOVY = new ScriptEngine("groovy",
        "org.codehaus.groovy.jsr223.GroovyScriptEngineFactory",
        List.of("org.codehaus.groovy.control.CompilationFailedException", // groovy
            "org.codehaus.groovy.jsr223.GroovyScriptEngineFactory",       // groovy-jsr223
            "groovy.json.JsonSlurper",                                    // groovy-json
            "groovy.xml.XmlSlurper",                                      // groovy-xml
            "groovy.sql.Sql",                                             // groovy-sql
            "groovy.text.SimpleTemplateEngine",                           // groovy-templates
            "groovy.time.TimeCategory"));                                 // groovy-dateutil

    public static final ScriptEngine JAVASCRIPT = new ScriptEngine("javascript",
        "org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory",
        // Nashorn generates bytecode at run time and needs ASM, which it does not shade
        List.of("org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory", // nashorn-core
            "org.objectweb.asm.ClassWriter",                                    // asm
            "org.objectweb.asm.util.Printer",                                   // asm-util
            "org.objectweb.asm.commons.Method",                                 // asm-commons
            "org.objectweb.asm.tree.ClassNode",                                 // asm-tree
            "org.objectweb.asm.tree.analysis.Analyzer"));                       // asm-analysis

    private final LocalAgentWorkspace workspace;

    public ScriptEngineLibraries(LocalAgentWorkspace workspace) {
        this.workspace = workspace;
    }

    /**
     * @return the directory holding the libraries of this engine, to be used as {@code plugins.<language>.libs}, or
     * {@code null} if the engine is not part of this application. Returning null leaves the property unset, which is
     * exactly what happens on a controller without the engine: the keyword then fails on the agent with "no script
     * engine", which says more than a missing directory would.
     */
    public Path resolve(ScriptEngine engine) throws LocalAgentException {
        Map<String, URL> libraries = librariesOf(engine);
        if (libraries.isEmpty()) {
            logger.debug("The {} script engine is not part of this application", engine.language());
            return null;
        }

        Path directory = workspace.getInstalledLibrariesDirectory(engine.language(), Constants.STEP_VERSION_STRING);
        if (Files.isDirectory(directory)) {
            if (holdsAllOf(directory, libraries.values())) {
                logger.debug("Using the {} libraries already extracted in {}", engine.language(), directory);
                return directory;
            }
            // The directory is named after the version, which does not change between two builds of the same one: an
            // application rebuilt during development would otherwise go on sending the libraries of the first build,
            // for ever, and a fix to what is extracted would never reach the agents.
            logger.info("Replacing the {} libraries in {}: they are not the ones this application runs on.",
                engine.language(), directory);
            deleteQuietly(directory);
        }

        // Written aside and moved into place, so that an application interrupted half way cannot leave incomplete
        // libraries behind for the next run to send to an agent
        Path temporaryDirectory;
        try {
            Files.createDirectories(directory.getParent());
            temporaryDirectory = Files.createTempDirectory(directory.getParent(), directory.getFileName() + ".part");
        } catch (IOException e) {
            throw new LocalAgentException("Error while creating the directory of the " + engine.language() + " libraries", e);
        }
        try {
            logger.info("Extracting the {} libraries to {}...", engine.language(), directory);
            for (URL library : libraries.values()) {
                copyLibrary(library, temporaryDirectory);
            }
            Files.move(temporaryDirectory, directory, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            deleteQuietly(temporaryDirectory);
            if (Files.isDirectory(directory)) {
                // Another process running concurrently won the move, its libraries are as good as ours
                return directory;
            }
            throw new LocalAgentException("Error while extracting the " + engine.language() + " libraries", e);
        } catch (LocalAgentException e) {
            deleteQuietly(temporaryDirectory);
            throw e;
        }
        return directory;
    }

    /**
     * @return the libraries the engine is made of, keyed by location so that the markers of one library resolve to it
     * only once. Empty if the engine is not on the class path at all.
     */
    private static Map<String, URL> librariesOf(ScriptEngine engine) throws LocalAgentException {
        if (loadClass(engine.factoryClass()) == null) {
            return Map.of();
        }
        // Ordered, so that the same libraries are extracted in the same order twice. Keyed by the location as a
        // string: URL.equals() resolves host names, which has no business happening here.
        Map<String, URL> libraries = new LinkedHashMap<>();
        for (String markerClass : engine.markerClasses()) {
            Class<?> marker = loadClass(markerClass);
            if (marker == null) {
                logger.debug("{} is not part of this application, the corresponding {} module will not be sent to the"
                    + " agents", markerClass, engine.language());
                continue;
            }
            URL location = locationOf(marker);
            libraries.putIfAbsent(location.toString(), location);
        }
        return libraries;
    }

    /**
     * @return the location of the library providing the given class
     * @throws LocalAgentException if it cannot be located, or if it is the application itself. The latter is the case
     *                             when the application is packaged as a single merged jar: the libraries no longer
     *                             exist as such in it, and copying that jar would send the whole application to the
     *                             agent. Rebuilding a library out of a merged jar is what this class used to do, and
     *                             what it deliberately no longer does.
     */
    private static URL locationOf(Class<?> clazz) throws LocalAgentException {
        URL location = codeSourceOf(clazz);
        if (location == null) {
            throw new LocalAgentException("Unable to locate the library providing " + clazz.getName());
        }
        URL own = codeSourceOf(ScriptEngineLibraries.class);
        if (own != null && own.toString().equals(location.toString())) {
            throw new LocalAgentException("The script engines cannot be provided to the agents: " + clazz.getName()
                + " is packaged in " + location + ", the application itself. The libraries of an engine have to remain"
                + " separate jars, which is not the case in a single merged jar. Configure plugins.<language>.libs to"
                + " point at the engines instead.");
        }
        return location;
    }

    private static URL codeSourceOf(Class<?> clazz) {
        CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
        return codeSource != null ? codeSource.getLocation() : null;
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className, false, ScriptEngineLibraries.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    /**
     * Copies one library into the target directory, under the name it has, from the two layouts an application can be
     * started from: the jars of a class path, and the jars nested in an executable jar which keeps its dependencies
     * (Spring Boot's {@code BOOT-INF/lib}). A nested jar is not reachable through the file system, hence reading it
     * out of the archive it is stored in.
     */
    // Package private for the sake of the tests, which cover both layouts without a packaged application
    static void copyLibrary(URL location, Path targetDirectory) throws IOException, LocalAgentException {
        Path target = targetDirectory.resolve(libraryFileName(location));
        if ("file".equals(location.getProtocol())) {
            Files.copy(fileLibrary(location), target, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        copyNestedLibrary(location, target);
    }

    /**
     * @return whether the directory holds every one of these libraries, which is what makes an extraction of a
     * previous run reusable
     */
    private static boolean holdsAllOf(Path directory, Iterable<URL> libraries) throws LocalAgentException {
        for (URL library : libraries) {
            if (!Files.isRegularFile(directory.resolve(libraryFileName(library)))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the name the library is copied under, which is the name it has: an engine locates its modules by their
     * own means, and a renamed jar is one more thing to explain when something does not load.
     */
    private static String libraryFileName(URL location) throws LocalAgentException {
        if ("file".equals(location.getProtocol())) {
            return fileLibrary(location).getFileName().toString();
        }
        if ("jar".equals(location.getProtocol())) {
            String entryName = nestedEntryName(location);
            return entryName.substring(entryName.lastIndexOf('/') + 1);
        }
        throw new LocalAgentException("Unable to read the library " + location + ": unsupported location.");
    }

    private static Path fileLibrary(URL location) throws LocalAgentException {
        try {
            return Path.of(location.toURI());
        } catch (URISyntaxException e) {
            throw new LocalAgentException("Unable to read the library " + location, e);
        }
    }

    /**
     * Reads a library out of the archive holding it, given a location of the form
     * {@code jar:file:/path/app.jar!/BOOT-INF/lib/library.jar!/}.
     */
    private static void copyNestedLibrary(URL location, Path target) throws IOException, LocalAgentException {
        Path archive = nestedArchive(location);
        String entryName = nestedEntryName(location);
        try (ZipFile jar = new ZipFile(archive.toFile())) {
            ZipEntry entry = jar.getEntry(entryName);
            if (entry == null) {
                throw new LocalAgentException("Unable to read the library " + location + ": " + entryName
                    + " is not in " + archive + ".");
            }
            try (InputStream content = jar.getInputStream(entry)) {
                Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static Path nestedArchive(URL location) throws LocalAgentException {
        try {
            return Path.of(URI.create(nestedParts(location)[0]));
        } catch (RuntimeException e) {
            throw new LocalAgentException("Unable to read the library " + location, e);
        }
    }

    private static String nestedEntryName(URL location) throws LocalAgentException {
        return nestedParts(location)[1];
    }

    private static String[] nestedParts(URL location) throws LocalAgentException {
        String[] parts = location.toString().substring("jar:".length()).split("!/");
        if (parts.length < 2 || parts[1].isEmpty()) {
            throw new LocalAgentException("Unable to read the library " + location + ": no nested library in it.");
        }
        return parts;
    }

    private static void deleteQuietly(Path directory) {
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    logger.debug("Failed to delete {}", path, e);
                }
            });
        } catch (IOException e) {
            logger.debug("Failed to clean up {}", directory, e);
        }
    }
}
