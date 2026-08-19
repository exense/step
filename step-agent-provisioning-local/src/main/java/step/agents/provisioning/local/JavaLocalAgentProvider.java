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
import step.core.agents.AgentTypeConstants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Starts the Java agent as a separate JVM.
 * <p>
 * The agent runs with its own classpath and <b>not</b> with the classpath of the CLI. This is the entire point of the
 * exercise: running keywords in the CLI's own JVM, as the local execution used to, exposes them to every library the
 * CLI happens to bring, whereas the agent isolates each keyword in its own class loader.
 * <p>
 * The agent is shipped inside the CLI as a plain resource — a nested jar the CLI never loads — and extracted on first
 * use. Being a resource rather than a dependency is what keeps it off the CLI's classpath; the same trick is used one
 * level down by {@code step-agent}, which embeds {@code step-grid-agent.jar} and extracts it at start-up.
 * <p>
 * No agent type brings its own runtime, but this is the only one whose runtime is there by construction: the CLI
 * starting the agent is itself a Java application, so a JVM is guaranteed to be available and the very one running
 * the CLI is reused. The other types need a runtime the developer installed, and are only offered when it is found.
 */
public class JavaLocalAgentProvider implements LocalAgentProvider {

    private static final Logger logger = LoggerFactory.getLogger(JavaLocalAgentProvider.class);
    private static final String AGENT_MAIN_CLASS = "step.grid.agent.AgentRunner";
    private static final String AGENT_CONF_FILE_NAME = "AgentConf.yaml";
    private static final String LOGBACK_CONF_FILE_NAME = "logback-local-agent.xml";
    /** The agent bundle embedded in the CLI jar. Kept in sync by the build, see the launcher's pom. */
    static final String EMBEDDED_AGENT_RESOURCE = "step-local-java-agent.jar";
    private static final String INSTALLED_AGENT_NAME = "java";
    private static final String INSTALLED_AGENT_JAR_NAME = "step-agent.jar";
    private static final String LIB_DIRECTORY_NAME = "lib";

    private final LocalAgentProvisioningConfiguration configuration;
    private final LocalAgentWorkspace workspace;
    private final AgentConfWriter agentConfWriter = new AgentConfWriter();

    public JavaLocalAgentProvider(LocalAgentProvisioningConfiguration configuration, LocalAgentWorkspace workspace) {
        this.configuration = configuration;
        this.workspace = workspace;
    }

    @Override
    public String getAgentType() {
        return AgentTypeConstants.AGENT_TYPE_JAVA;
    }

    @Override
    public String getDisplayName() {
        return "Java";
    }

    @Override
    public boolean isAvailable() {
        return configuration.getJavaAgentPath() != null || isEmbedded();
    }

    private static boolean isEmbedded() {
        return JavaLocalAgentProvider.class.getClassLoader().getResource(EMBEDDED_AGENT_RESOURCE) != null;
    }

    @Override
    public LocalAgentProcess start(LocalAgentStartContext context) throws LocalAgentException {
        List<String> classpath = resolveAgentClasspath();

        Path runDirectory = context.getWorkingDirectory();
        Path agentConf;
        try {
            agentConf = agentConfWriter.write(runDirectory, AGENT_CONF_FILE_NAME, context, Map.of(
                // The local agents never fork: they are already the isolated process the CLI provisioned for this
                // execution, and a second level of forking would only add a start-up delay per keyword.
                "agentForker", Map.of("enabled", false),
                // Settings of the Java agent rather than of every agent, hence written here and not by the writer
                "gridReadTimeout", AgentConfWriter.GRID_READ_TIMEOUT_MS,
                "ssl", false));
        } catch (IOException e) {
            throw new LocalAgentException("Error while writing the configuration of the local Java agent", e);
        }

        List<String> command = new ArrayList<>();
        command.add(currentJavaExecutable());
        command.add("-Dlogback.configurationFile=" + writeLogbackConfiguration(runDirectory).toAbsolutePath());
        command.add("-Dstep.localAgent.logLevel=" + (configuration.isDebug() ? "debug" : "info"));
        command.addAll(vmArgs());
        command.add("-cp");
        command.add(String.join(File.pathSeparator, classpath));
        command.add(AGENT_MAIN_CLASS);
        command.add("-config=" + agentConf.toAbsolutePath());

        logger.debug("Starting the local Java agent with command: {}", command);
        Process process;
        try {
            process = new ProcessBuilder(command)
                .directory(runDirectory.toFile())
                // Merged so that a failure printed on stderr ends up in the retained output and can be reported
                .redirectErrorStream(true)
                .start();
        } catch (IOException e) {
            throw new LocalAgentException("Error while starting the local Java agent", e);
        }
        return new LocalAgentProcess("Local " + getDisplayName() + " agent", process, runDirectory,
            configuration.isVerbose());
    }

    /**
     * Gives the agent a logging configuration of its own, writing to the console the CLI reads. Without one, logback
     * falls back to its default, which prints every DEBUG statement of every library the agent contains.
     */
    private Path writeLogbackConfiguration(Path runDirectory) throws LocalAgentException {
        Path logbackConfiguration = runDirectory.resolve(LOGBACK_CONF_FILE_NAME);
        try (InputStream template = JavaLocalAgentProvider.class.getClassLoader().getResourceAsStream(LOGBACK_CONF_FILE_NAME)) {
            if (template == null) {
                throw new LocalAgentException("The logging configuration of the local agents is missing from the CLI");
            }
            Files.copy(template, logbackConfiguration, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new LocalAgentException("Error while writing the logging configuration of the local Java agent", e);
        }
        return logbackConfiguration;
    }

    /**
     * @return the classpath of the agent: either the {@code lib} directory of an agent installation the user pointed
     * at, or the single bundle extracted from the CLI.
     */
    private List<String> resolveAgentClasspath() throws LocalAgentException {
        Path configuredAgent = configuration.getJavaAgentPath();
        if (configuredAgent != null) {
            Path libDirectory = configuredAgent.resolve(LIB_DIRECTORY_NAME);
            if (!containsAtLeastOneJar(libDirectory)) {
                throw new LocalAgentException("The configured Java agent " + configuredAgent
                    + " does not look like an agent installation: no jar found in " + libDirectory);
            }
            // Expanded by the JVM itself, which spares us from listing the jars
            return List.of(libDirectory.toAbsolutePath() + File.separator + "*");
        }
        return List.of(extractEmbeddedAgent().toAbsolutePath().toString());
    }

    /**
     * Extracts the embedded agent bundle, once per CLI version. The extracted bundle is kept between runs: it is
     * ~20 MB and rewriting it on every local execution would be pure waste.
     */
    private Path extractEmbeddedAgent() throws LocalAgentException {
        Path installedDirectory = workspace.getInstalledAgentDirectory(INSTALLED_AGENT_NAME, Constants.STEP_VERSION_STRING);
        Path agentJar = installedDirectory.resolve(INSTALLED_AGENT_JAR_NAME);
        // The directory is named after the version, which does not change between two builds of the same one. An
        // extracted agent is therefore only reused when it is also the same size as the embedded one, otherwise
        // every CLI rebuilt during development would go on running the agent of the first build, for ever.
        long embeddedSize = embeddedAgentSize();
        if (Files.isRegularFile(agentJar) && sizeOf(agentJar) == embeddedSize) {
            logger.debug("Using the Java agent already extracted in {}", installedDirectory);
            return agentJar;
        }

        logger.info("Extracting the Java agent to {}...", installedDirectory);
        try (InputStream embedded = JavaLocalAgentProvider.class.getClassLoader().getResourceAsStream(EMBEDDED_AGENT_RESOURCE)) {
            if (embedded == null) {
                throw new LocalAgentException("This CLI does not embed the Java agent, and no Java agent was configured."
                    + " Point --localAgentJava at an unpacked Step agent installation.");
            }
            Files.createDirectories(installedDirectory);
            // Written next to the target and moved into place, so that a CLI interrupted mid-extraction (or a
            // second CLI running concurrently) can never leave a truncated jar behind for the next run to use.
            Path temporaryJar = Files.createTempFile(installedDirectory, INSTALLED_AGENT_JAR_NAME, ".part");
            try {
                Files.copy(embedded, temporaryJar, StandardCopyOption.REPLACE_EXISTING);
                Files.move(temporaryJar, agentJar, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporaryJar);
            }
        } catch (IOException e) {
            throw new LocalAgentException("Error while extracting the Java agent to " + installedDirectory, e);
        }
        return agentJar;
    }

    /**
     * @return the size of the agent embedded in this CLI, or -1 when it cannot be determined. Read from the jar
     * index rather than by reading the 20 MB of the agent itself.
     */
    private static long embeddedAgentSize() {
        URL resource = JavaLocalAgentProvider.class.getClassLoader().getResource(EMBEDDED_AGENT_RESOURCE);
        if (resource == null) {
            return -1;
        }
        try {
            return resource.openConnection().getContentLengthLong();
        } catch (IOException e) {
            logger.debug("Unable to determine the size of the embedded Java agent", e);
            return -1;
        }
    }

    private static long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            logger.debug("Unable to determine the size of {}", file, e);
            return -1;
        }
    }

    private static boolean containsAtLeastOneJar(Path libDirectory) {
        if (!Files.isDirectory(libDirectory)) {
            return false;
        }
        try (Stream<Path> entries = Files.list(libDirectory)) {
            return entries.anyMatch(p -> p.getFileName().toString().endsWith(".jar"));
        } catch (IOException e) {
            throw new UncheckedIOException("Error while reading the agent library directory " + libDirectory, e);
        }
    }

    /**
     * @return the java executable running this CLI. Reusing it rather than looking JAVA_HOME up guarantees that the
     * agent runs on the very JVM the developer started the CLI with.
     */
    private static String currentJavaExecutable() throws LocalAgentException {
        return ProcessHandle.current().info().command().orElseThrow(() -> new LocalAgentException(
            "Unable to determine the java executable of the current process, which is needed to start the local Java agent."));
    }

    private List<String> vmArgs() {
        return Optional.ofNullable(configuration.getJavaAgentVmArgs())
            .filter(args -> !args.isBlank())
            .map(args -> List.of(args.trim().split("\\s+")))
            .orElseGet(List::of);
    }
}
