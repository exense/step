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
import step.core.agents.AgentTypeConstants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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
            agentConf = agentConfWriter.write(runDirectory, AGENT_CONF_FILE_NAME, context,
                // The local agents never fork: they are already the isolated process the CLI provisioned for this
                // execution, and a second level of forking would only add a start-up delay per keyword.
                Map.of("agentForker", Map.of("enabled", false)));
        } catch (IOException e) {
            throw new LocalAgentException("Error while writing the configuration of the local Java agent", e);
        }

        List<String> command = new ArrayList<>();
        command.add(currentJavaExecutable());
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
        return new LocalAgentProcess("Local " + getDisplayName() + " agent", process, runDirectory);
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
        if (Files.isRegularFile(agentJar)) {
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
