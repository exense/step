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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Constants;
import step.core.agents.AgentTypeConstants;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;

/**
 * Starts the Node.js agent as a separate process.
 * <p>
 * The agent is shipped inside the CLI, with its dependencies already resolved, and extracted on first use, as the Java agent is.
 * <p>
 * There are two ways to get an agent, the same two the Java agent offers: the one embedded here, and the installation
 * {@code --localAgentNode} points at, which takes precedence. Both are started as {@code node server.js}, which is
 * all {@code bin/step-node-agent} does - the commands npm generates are specific to the machine they were generated
 * on, and the embedded agent is packed on one machine for all of them.
 * <p>
 * Unlike the Java agent, whose runtime the CLI provides by construction, this one depends on what the developer
 * machine offers: {@code node} has to be runnable. When it is not, the agent type is simply not offered and a plan
 * using JavaScript keywords fails with "no agent available" rather than with an obscure process error.
 */
public class NodeLocalAgentProvider implements LocalAgentProvider {

    private static final Logger logger = LoggerFactory.getLogger(NodeLocalAgentProvider.class);
    private static final String AGENT_CONF_FILE_NAME = "AgentConf.yaml";
    private static final String NPM_PACKAGE_NAME = "step-node-agent";
    private static final String NODE_MODULES_DIRECTORY_NAME = "node_modules";
    /**
     * The agent bundle embedded in the CLI: the agent and its runtime dependencies, as resolved by the build. Kept in
     * sync by the build, see the launcher pom.
     */
    static final String EMBEDDED_AGENT_RESOURCE = "step-local-node-agent.tar.gz";
    private static final String INSTALLED_EMBEDDED_AGENT_NAME = "node";
    private static final String EMBEDDED_AGENT_IDENTITY_FILE_NAME = ".embedded-agent-id";
    /**
     * What {@code bin/step-node-agent} requires, and therefore what starting the agent comes down to.
     */
    private static final String AGENT_MAIN_SCRIPT = "server.js";

    private final LocalAgentProvisioningConfiguration configuration;
    private final LocalAgentWorkspace workspace;
    private final AgentConfWriter agentConfWriter = new AgentConfWriter();

    public NodeLocalAgentProvider(LocalAgentProvisioningConfiguration configuration, LocalAgentWorkspace workspace) {
        this.configuration = configuration;
        this.workspace = workspace;
    }

    @Override
    public String getAgentType() {
        return AgentTypeConstants.AGENT_TYPE_NODEJS;
    }

    @Override
    public String getDisplayName() {
        return "Node.js";
    }

    @Override
    public boolean isAvailable() {
        return OsCommands.isExecutableAvailable(OsCommands.NODE)
            && (configuration.getNodeAgentPath() != null || isAgentEmbedded());
    }

    @Override
    public String getInstallationHint() {
        if (!OsCommands.isExecutableAvailable(OsCommands.NODE)) {
            return "The Node.js agent runs on Node.js, which is not installed on this machine.";
        }
        return "This CLI does not embed the Node.js agent: point --localAgentNode at an installed "
            + NPM_PACKAGE_NAME + ".";
    }

    private static boolean isAgentEmbedded() {
        return embeddedAgentResource() != null;
    }

    private static URL embeddedAgentResource() {
        return NodeLocalAgentProvider.class.getClassLoader().getResource(EMBEDDED_AGENT_RESOURCE);
    }

    @Override
    public LocalAgentProcess start(LocalAgentStartContext context) throws LocalAgentException {
        // Resolved first: an agent which has to be installed, or which cannot be found at all, must fail before a port
        // is reserved and a configuration written for it.
        List<String> command = new ArrayList<>(resolveAgentCommand());

        Path runDirectory = context.getWorkingDirectory();
        // Unlike the Java agent, the Node.js agent falls back to a fixed port (3000) when none is configured and
        // never picks a free one itself, so the port is reserved here and pinned together with the URL.
        int agentPort = LocalPorts.findFreeLoopbackPort();
        Path agentConf;
        try {
            agentConf = agentConfWriter.write(runDirectory, AGENT_CONF_FILE_NAME, context, Map.of(
                "agentPort", agentPort,
                "agentUrl", "http://" + AgentConfWriter.LOOPBACK_HOST + ":" + agentPort,
                // Read by the file manager of this agent, which otherwise gives every download from the grid the 3s
                // it defaults to. Written here rather than by the writer: not every agent supports it.
                "gridReadTimeout", AgentConfWriter.GRID_READ_TIMEOUT_MS));
        } catch (IOException e) {
            throw new LocalAgentException("Error while writing the configuration of the local Node.js agent", e);
        }

        command.add("-f");
        command.add(agentConf.toAbsolutePath().toString());
        logger.debug("Starting the local Node.js agent with command: {}", command);
        Process process;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(runDirectory.toFile())
                .redirectErrorStream(true);
            // The agent logs to the console through winston, at the level of this variable
            processBuilder.environment().put("LOG_LEVEL", configuration.isDebug() ? "debug" : "info");
            process = processBuilder.start();
        } catch (IOException e) {
            throw new LocalAgentException("Error while starting the local Node.js agent", e);
        }
        return new LocalAgentProcess("Local " + getDisplayName() + " agent", process, runDirectory,
            configuration.isVerbose());
    }

    /**
     * @return the command starting the agent, without its arguments
     */
    // Package private for the sake of the tests, which cover the resolution without starting anything
    List<String> resolveAgentCommand() throws LocalAgentException {
        return List.of(OsCommands.NODE, resolveAgentScript().toAbsolutePath().toString());
    }

    /**
     * @return the script starting the agent: the one of the installation the user pointed at, or the one of the agent
     * embedded in this CLI
     */
    private Path resolveAgentScript() throws LocalAgentException {
        Path configured = configuration.getNodeAgentPath();
        if (configured != null) {
            logger.debug("Using the Node.js agent configured in {}", configured);
            return validateConfiguredAgent(configured);
        }
        return extractEmbeddedAgent();
    }

    /**
     * Extracts the embedded agent, once per build of the CLI, and keeps it between runs: it is a few thousand files
     * and unpacking them on every local execution would be pure waste.
     * <p>
     * The directory is named after the version, which is the same for every build of that version, so what it was
     * extracted from is recorded next to it and it is reused only while that still matches.
     *
     * @return the script starting the agent
     */
    private Path extractEmbeddedAgent() throws LocalAgentException {
        URL resource = embeddedAgentResource();
        if (resource == null) {
            throw new LocalAgentException("This CLI does not embed the Node.js agent, and none was configured."
                + " Point --localAgentNode at an installed " + NPM_PACKAGE_NAME + ".");
        }

        Path directory = workspace.getInstalledAgentDirectory(INSTALLED_EMBEDDED_AGENT_NAME, Constants.STEP_VERSION_STRING);
        Path mainScript = directory.resolve(AGENT_MAIN_SCRIPT);
        String identity = identifyEmbeddedAgent(resource);
        if (Files.isRegularFile(mainScript) && identity != null && identity.equals(readIdentity(directory))) {
            logger.debug("Using the Node.js agent already extracted in {}", directory);
            return mainScript;
        }

        logger.info("Extracting the Node.js agent to {}...", directory);
        // Unpacked aside and moved into place, so that a CLI interrupted half way, or a second CLI running
        // concurrently, can never leave a partial agent behind for the next run to start.
        Path temporaryDirectory;
        try {
            deleteQuietly(directory);
            Files.createDirectories(directory.getParent());
            temporaryDirectory = Files.createTempDirectory(directory.getParent(), directory.getFileName() + ".part");
        } catch (IOException e) {
            throw new LocalAgentException("Error while creating the directory of the Node.js agent " + directory, e);
        }
        try {
            unpack(resource, temporaryDirectory);
            if (identity != null) {
                // Written before the move, so that the identity and what it describes become visible together
                Files.writeString(temporaryDirectory.resolve(EMBEDDED_AGENT_IDENTITY_FILE_NAME), identity);
            }
            Files.move(temporaryDirectory, directory, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            deleteQuietly(temporaryDirectory);
            throw new LocalAgentException("Error while extracting the Node.js agent to " + directory, e);
        }
        if (!Files.isRegularFile(mainScript)) {
            throw new LocalAgentException("The Node.js agent embedded in this CLI contains no " + AGENT_MAIN_SCRIPT
                + ": " + mainScript + " does not exist after extracting it.");
        }
        return mainScript;
    }

    // Package private for the sake of the tests, which unpack an archive of their own rather than the embedded agent
    static void unpack(URL archive, Path directory) throws IOException {
        try (InputStream stream = archive.openStream();
             TarArchiveInputStream entries = new TarArchiveInputStream(
                 new GzipCompressorInputStream(new BufferedInputStream(stream)))) {
            TarArchiveEntry entry;
            while ((entry = entries.getNextEntry()) != null) {
                if (!entries.canReadEntryData(entry)) {
                    logger.debug("Skipping the unreadable entry {} of the embedded Node.js agent", entry.getName());
                    continue;
                }
                Path target = directory.resolve(entry.getName()).normalize();
                if (!target.startsWith(directory)) {
                    // An entry pointing outside the directory it is unpacked into. Nothing produces one here, the
                    // archive being built by our own build, but unpacking one would write anywhere on the machine.
                    throw new IOException("The embedded Node.js agent contains an entry outside of the archive: "
                        + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(entries, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * @return what identifies the agent this CLI embeds, or {@code null} when it cannot be determined, which extracts
     * it afresh on every run rather than risking a stale one. Read from the index of the CLI jar when there is one,
     * so that identifying it costs no read of the archive; computed from the archive itself otherwise, which is the
     * case when the CLI runs from compiled classes rather than from a jar.
     */
    private static String identifyEmbeddedAgent(URL resource) {
        try {
            if (resource.openConnection() instanceof JarURLConnection jarConnection) {
                JarEntry entry = jarConnection.getJarEntry();
                if (entry != null && entry.getCrc() != -1) {
                    return entry.getSize() + ":" + entry.getCrc();
                }
            }
            return checksumOf(resource);
        } catch (IOException e) {
            logger.debug("Unable to identify the Node.js agent embedded in this CLI", e);
            return null;
        }
    }

    private static String checksumOf(URL resource) throws IOException {
        try (InputStream stream = new BufferedInputStream(resource.openStream())) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 is not available on this JVM", e);
        }
    }

    /**
     * @return the identity recorded next to an extracted agent, or {@code null} when there is none to read
     */
    private static String readIdentity(Path directory) {
        Path identityFile = directory.resolve(EMBEDDED_AGENT_IDENTITY_FILE_NAME);
        try {
            return Files.readString(identityFile).trim();
        } catch (IOException e) {
            logger.debug("Unable to read the identity of the extracted Node.js agent from {}", identityFile, e);
            return null;
        }
    }

    /**
     * Checks what the user pointed at up front, rather than letting the process fail: a missing agent is otherwise
     * reported as a start-up timeout, with the actual cause buried in the output of a process that died seconds
     * earlier.
     * <p>
     * Both shapes an installed agent comes in are accepted: the package itself, which is what a global
     * {@code npm install -g step-node-agent} and an unpacked archive leave, and a project the agent was installed
     * into, which is what {@code npm install ./step-node-agent-<version>.tgz} leaves. Either way the dependencies of
     * the agent are found by the ordinary lookup of node, from the directory the script sits in upwards.
     *
     * @return the script starting the configured agent
     */
    private static Path validateConfiguredAgent(Path configured) throws LocalAgentException {
        Path packageScript = configured.resolve(AGENT_MAIN_SCRIPT);
        if (Files.isRegularFile(packageScript)) {
            return packageScript;
        }
        Path projectScript = configured.resolve(NODE_MODULES_DIRECTORY_NAME).resolve(NPM_PACKAGE_NAME)
            .resolve(AGENT_MAIN_SCRIPT);
        if (Files.isRegularFile(projectScript)) {
            return projectScript;
        }
        throw new LocalAgentException("The configured Node.js agent " + configured + " does not look like an agent"
            + " installation: neither " + packageScript + " nor " + projectScript + " exists. Point --localAgentNode"
            + " at an installed " + NPM_PACKAGE_NAME + " package, or at a directory containing it in "
            + NODE_MODULES_DIRECTORY_NAME + ".");
    }

    private static void deleteQuietly(Path directory) {
        try {
            FileUtils.deleteDirectory(directory.toFile());
        } catch (IOException e) {
            logger.warn("Failed to clean up {} after a failed extraction", directory, e);
        }
    }

    /**
     * Small helpers around the external commands this provider depends on.
     */
    static class OsCommands {

        static final String NODE = "node";

        private static final long VERSION_CHECK_TIMEOUT_MS = 10_000;
        private static final Map<String, Boolean> AVAILABILITY_CACHE = new ConcurrentHashMap<>();

        /**
         * Reports whether an executable can actually be run, by running {@code <executable> --version}.
         * <p>
         * Asking the command itself rather than looking it up in the PATH is the only reliable answer: a runtime
         * installed through a version manager is often a shim or a wrapper that a directory scan does not recognize,
         * and a PATH is not even guaranteed to contain valid paths. The result is cached, since this is called while
         * building the list of available agent types on every local execution, including those not using Node.js.
         */
        static boolean isExecutableAvailable(String executable) {
            return AVAILABILITY_CACHE.computeIfAbsent(executable, OsCommands::runVersionCheck);
        }

        private static boolean runVersionCheck(String executable) {
            try {
                Result result = run(List.of(executable, "--version"), null, VERSION_CHECK_TIMEOUT_MS);
                if (result.exitCode() == 0) {
                    logger.debug("Found {} {}", executable, result.output().trim());
                    return true;
                }
                logger.debug("{} --version exited with {}", executable, result.exitCode());
            } catch (IOException e) {
                // The usual case: the executable is simply not installed
                logger.debug("{} is not available on this machine", executable, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return false;
        }

        /**
         * @param directory the working directory, or {@code null} to inherit the one of the CLI
         */
        static Result run(List<String> command, Path directory, long timeoutMs) throws IOException, InterruptedException {
            ProcessBuilder processBuilder = new ProcessBuilder(command).redirectErrorStream(true);
            if (directory != null) {
                processBuilder.directory(directory.toFile());
            }
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            Thread reader = new Thread(() -> {
                try (var in = process.getInputStream()) {
                    output.append(new String(in.readAllBytes()));
                } catch (IOException e) {
                    logger.debug("Error while reading the output of {}", command.get(0), e);
                }
            });
            reader.setDaemon(true);
            reader.start();

            if (!process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IOException(command.get(0) + " did not complete within " + timeoutMs + "ms");
            }
            reader.join(5000);
            return new Result(process.exitValue(), output.toString());
        }

        record Result(int exitCode, String output) {
        }
    }
}
