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
package step.cli.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * Provides the script engine libraries a Groovy or JavaScript keyword needs, so that they can be sent to the agent
 * executing it.
 * <p>
 * These keywords run through {@code javax.script}, and their engine has to be on the class path of the <b>agent</b>.
 * A Step controller ships the engines in {@code ext/groovy} and {@code ext/javascript} and points
 * {@code plugins.<language>.libs} at them, which is what makes them travel to the agent as the keyword's plugin
 * libraries. The CLI has no such directory, and this class builds the equivalent.
 * <p>
 * The libraries are not shipped a second time inside the CLI: it is a fat jar which already contains the engines,
 * because step-core depends on {@code groovy-all} and {@code nashorn-core} for its own expression handling. They are
 * therefore extracted back out of it into a jar, once per CLI version. The alternative, embedding the two library
 * jars as resources the way the Java agent is embedded, would carry ~29 MB of classes the CLI already holds.
 * <p>
 * Selecting the entries of an engine out of a merged fat jar means naming its packages, which this class does per
 * engine. It is checked by {@code ScriptEngineLibrariesTest}, which fails if an engine upgrade moves or renames them.
 */
public class ScriptEngineLibraries {

    private static final Logger logger = LoggerFactory.getLogger(ScriptEngineLibraries.class);
    private static final String SCRIPT_ENGINE_SERVICE = "META-INF/services/javax.script.ScriptEngineFactory";

    /**
     * An engine, as found in the CLI's own class path.
     *
     * @param language      the language as Step names it, i.e. the {@code plugins.<language>.libs} property
     * @param factoryClass  the {@code ScriptEngineFactory}. It is how the engine is detected, and what the generated
     *                      service file has to declare.
     * @param entryFilter   selects the entries belonging to this engine among those of the jars it is taken from
     * @param markerClasses one class per library the engine is spread over, used to locate those libraries when the
     *                      CLI does not run from its fat jar. Absent ones are skipped, so this list can name modules
     *                      which are not always there. It has no effect on a packaged CLI, where the filter alone
     *                      decides and therefore takes everything the engine ships with.
     */
    public record ScriptEngine(String language, String factoryClass, Predicate<String> entryFilter,
                               List<String> markerClasses) {
    }

    public static final ScriptEngine GROOVY = new ScriptEngine("groovy",
        "org.codehaus.groovy.jsr223.GroovyScriptEngineFactory",
        entry -> startsWithAny(entry, "groovy/", "groovyjarjar", "org/codehaus/groovy/", "org/apache/groovy/",
            // The extension modules (groovy-dateutil and friends), the AST transformations and the runner services.
            // Groovy silently loses the corresponding features without them, rather than failing to start.
            "META-INF/groovy/", "META-INF/groovy-release-info.properties",
            "META-INF/services/org.codehaus.groovy.", "META-INF/services/org.apache.groovy."),
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
        entry -> startsWithAny(entry, "org/openjdk/nashorn/", "org/objectweb/asm/"),
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
     * {@code null} if the engine is not part of this CLI. Returning null leaves the property unset, which is exactly
     * what happens on a controller without the engine: the keyword then fails on the agent with "no script engine",
     * which says more than a missing directory would.
     */
    public Path resolve(ScriptEngine engine) throws LocalAgentException {
        Set<Path> sources = codeSourcesOf(engine);
        if (sources.isEmpty()) {
            logger.debug("The {} script engine is not part of this CLI", engine.language());
            return null;
        }

        Path directory = workspace.getInstalledLibrariesDirectory(engine.language(), Constants.STEP_VERSION_STRING);
        if (Files.isDirectory(directory)) {
            logger.debug("Using the {} libraries already extracted in {}", engine.language(), directory);
            return directory;
        }

        // Written aside and moved into place, so that a CLI interrupted half way cannot leave incomplete libraries
        // behind for the next run to send to an agent
        Path temporaryDirectory;
        try {
            Files.createDirectories(directory.getParent());
            temporaryDirectory = Files.createTempDirectory(directory.getParent(), directory.getFileName() + ".part");
        } catch (IOException e) {
            throw new LocalAgentException("Error while creating the directory of the " + engine.language() + " libraries", e);
        }
        try {
            logger.info("Extracting the {} libraries to {}...", engine.language(), directory);
            copyLibraries(engine, sources, temporaryDirectory);
            Files.move(temporaryDirectory, directory, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            deleteQuietly(temporaryDirectory);
            if (Files.isDirectory(directory)) {
                // Another CLI running concurrently won the move, its libraries are as good as ours
                return directory;
            }
            throw new LocalAgentException("Error while extracting the " + engine.language() + " libraries", e);
        }
        return directory;
    }

    /**
     * Builds the single jar sent to the agent, by copying the entries of the engine out of the libraries it was
     * found in. Those are one fat jar for a packaged CLI and one jar per engine module otherwise, but the selection
     * is the same in both cases, so a development run exercises the same filtering as a released CLI.
     */
    private void copyLibraries(ScriptEngine engine, Set<Path> sources, Path targetDirectory) throws IOException {
        Path target = targetDirectory.resolve(engine.language() + "-libraries.jar");
        Set<String> written = new LinkedHashSet<>();
        try (OutputStream out = Files.newOutputStream(target);
             JarOutputStream libraries = new JarOutputStream(out)) {
            for (Path source : sources) {
                copyEntries(engine, source, libraries, written);
            }
            // A fat jar declares every engine it contains in a single merged service file, and a module of the engine
            // may declare it too. The agent must be offered exactly one, hence a service file written from scratch.
            libraries.putNextEntry(new ZipEntry(SCRIPT_ENGINE_SERVICE));
            libraries.write((engine.factoryClass() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            libraries.closeEntry();
        }
        if (written.isEmpty()) {
            throw new IOException("No " + engine.language() + " entry found in " + sources
                + ". The packages of the script engine have probably changed, see ScriptEngineLibraries.");
        }
        logger.debug("Collected {} {} entries from {}", written.size(), engine.language(), sources);
    }

    private static void copyEntries(ScriptEngine engine, Path source, JarOutputStream libraries, Set<String> written)
        throws IOException {
        try (JarFile jar = new JarFile(source.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                // Duplicates are possible across the modules of an engine, typically their service files. The first
                // one wins, as a jar cannot hold the same entry twice.
                if (entry.isDirectory() || !engine.entryFilter().test(name) || !written.add(name)) {
                    continue;
                }
                libraries.putNextEntry(new ZipEntry(name));
                try (InputStream content = jar.getInputStream(entry)) {
                    content.transferTo(libraries);
                }
                libraries.closeEntry();
            }
        }
    }

    /**
     * @return the jars the engine is made of, empty if it is not on the class path at all
     */
    private static Set<Path> codeSourcesOf(ScriptEngine engine) throws LocalAgentException {
        if (!isOnClassPath(engine.factoryClass())) {
            return Set.of();
        }
        // A set, as the modules of an engine all resolve to the same jar in a packaged CLI. Ordered, so that the
        // resulting jar is built the same way twice.
        Set<Path> sources = new LinkedHashSet<>();
        for (String markerClass : engine.markerClasses()) {
            if (isOnClassPath(markerClass)) {
                sources.add(codeSourceOf(loadClass(markerClass)));
            } else {
                logger.debug("{} is not part of this CLI, the corresponding {} module will not be sent to the agents",
                    markerClass, engine.language());
            }
        }
        return sources;
    }

    private static boolean isOnClassPath(String className) {
        return loadClass(className) != null;
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className, false, ScriptEngineLibraries.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    private static Path codeSourceOf(Class<?> clazz) throws LocalAgentException {
        CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            throw new LocalAgentException("Unable to locate the library providing " + clazz.getName());
        }
        try {
            return Path.of(codeSource.getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new LocalAgentException("Unable to locate the library providing " + clazz.getName(), e);
        }
    }

    private static boolean startsWithAny(String entry, String... prefixes) {
        return List.of(prefixes).stream().anyMatch(entry::startsWith);
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
