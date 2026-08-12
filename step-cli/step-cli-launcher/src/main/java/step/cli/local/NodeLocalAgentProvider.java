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

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.agents.AgentTypeConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Starts the Node.js agent as a separate process.
 * <p>
 * The agent is not shipped with the CLI: it is a set of JavaScript files, and since it needs a Node.js runtime on the
 * machine anyway, embedding it would only save an npm install while making every CLI carry it. It is instead
 * installed with npm into the workspace on first use and reused afterwards, or taken from an existing installation
 * the user points at.
 * <p>
 * Unlike the Java agent, whose runtime the CLI provides by construction, this one depends on what the developer
 * machine offers: {@code node} and {@code npm} have to be runnable. When they are not, the agent type is simply not
 * offered and a plan using JavaScript keywords fails with "no agent available" rather than with an obscure process
 * error.
 */
public class NodeLocalAgentProvider implements LocalAgentProvider {

    private static final Logger logger = LoggerFactory.getLogger(NodeLocalAgentProvider.class);
    private static final String AGENT_CONF_FILE_NAME = "AgentConf.yaml";
    private static final String NPM_PACKAGE_NAME = "step-node-agent";
    private static final String INSTALLED_AGENT_NAME = "node";
    private static final String AGENT_MAIN_SCRIPT = "server.js";
    private static final String NODE_MODULES_DIRECTORY_NAME = "node_modules";
    /**
     * One of the dependencies the agent requires at start-up. The published package bundles its dependencies, so an
     * installed agent always has them, whereas a source checkout has none until {@code npm install} is run in it.
     */
    private static final String AGENT_RUNTIME_DEPENDENCY = "express";
    private static final int NPM_INSTALL_TIMEOUT_MINUTES = 10;

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
        if (!OsCommands.isExecutableAvailable(OsCommands.NODE)) {
            return false;
        }
        // An already installed agent only needs node to run it; installing one additionally needs npm.
        return configuration.getNodeAgentPath() != null
            || isInstalled()
            || OsCommands.isExecutableAvailable(OsCommands.NPM);
    }

    private boolean isInstalled() {
        return isUsableAgentPackage(installedAgentDirectory());
    }

    private Path installedAgentDirectory() {
        return workspace.getInstalledAgentDirectory(INSTALLED_AGENT_NAME, configuration.getNodeAgentVersion())
            .resolve(NODE_MODULES_DIRECTORY_NAME).resolve(NPM_PACKAGE_NAME);
    }

    @Override
    public LocalAgentProcess start(LocalAgentStartContext context) throws LocalAgentException {
        Path mainScript = resolveAgentDirectory().resolve(AGENT_MAIN_SCRIPT);

        Path runDirectory = context.getWorkingDirectory();
        // Unlike the Java agent, the Node.js agent falls back to a fixed port (3000) when none is configured and
        // never picks a free one itself, so the port is reserved here and pinned together with the URL.
        int agentPort = OsCommands.findFreeLoopbackPort();
        Path agentConf;
        try {
            agentConf = agentConfWriter.write(runDirectory, AGENT_CONF_FILE_NAME, context, Map.of(
                "agentPort", agentPort,
                "agentUrl", "http://" + AgentConfWriter.LOOPBACK_HOST + ":" + agentPort));
        } catch (IOException e) {
            throw new LocalAgentException("Error while writing the configuration of the local Node.js agent", e);
        }

        List<String> command = List.of(OsCommands.NODE, mainScript.toAbsolutePath().toString(),
            "-f", agentConf.toAbsolutePath().toString());
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
     * @return the root of the agent package to start, which is guaranteed to be runnable
     */
    // Package private for the sake of the tests, which cover the layouts a user can point at without starting node
    Path resolveAgentDirectory() throws LocalAgentException {
        Path configured = configuration.getNodeAgentPath();
        if (configured != null) {
            return validateConfiguredAgent(configured);
        }
        Path installed = installedAgentDirectory();
        if (isUsableAgentPackage(installed)) {
            logger.debug("Using the Node.js agent already installed in {}", installed);
            return installed;
        }
        installWithNpm();
        if (!isUsableAgentPackage(installed)) {
            throw new LocalAgentException("npm reported a successful installation but no usable Node.js agent was"
                + " found in " + installed);
        }
        return installed;
    }

    /**
     * Checks an agent the user pointed at up front, rather than letting node fail on it. Both failure modes below are
     * otherwise reported as a start-up timeout, with the actual cause buried in the output of a process that died
     * seconds earlier.
     */
    private static Path validateConfiguredAgent(Path configured) throws LocalAgentException {
        Path packageRoot = findPackageRoot(configured);
        if (packageRoot == null) {
            throw new LocalAgentException("The configured Node.js agent " + configured + " contains no "
                + AGENT_MAIN_SCRIPT + ", neither directly nor in " + NODE_MODULES_DIRECTORY_NAME + "/" + NPM_PACKAGE_NAME
                + ". Point --localAgentNode at an installed " + NPM_PACKAGE_NAME + " package.");
        }
        if (!canResolveDependency(packageRoot, AGENT_RUNTIME_DEPENDENCY)) {
            throw new LocalAgentException("The Node.js agent in " + packageRoot + " has no installed dependencies,"
                + " which is what a source checkout looks like. Run 'npm install' in it, or point --localAgentNode at"
                + " an agent installed from the " + NPM_PACKAGE_NAME + " package.");
        }
        return packageRoot;
    }

    private static boolean isUsableAgentPackage(Path packageRoot) {
        return Files.isRegularFile(packageRoot.resolve(AGENT_MAIN_SCRIPT))
            && canResolveDependency(packageRoot, AGENT_RUNTIME_DEPENDENCY);
    }

    /**
     * Accepts the two layouts a user can reasonably point at: the agent package itself, or a directory containing it
     * under {@code node_modules}. The latter is what {@code npm install --prefix} produces, and it is the layout of
     * the installations this provider makes itself.
     *
     * @return the root of the agent package, or {@code null} if the directory holds no agent
     */
    private static Path findPackageRoot(Path directory) {
        if (Files.isRegularFile(directory.resolve(AGENT_MAIN_SCRIPT))) {
            return directory;
        }
        Path nested = directory.resolve(NODE_MODULES_DIRECTORY_NAME).resolve(NPM_PACKAGE_NAME);
        return Files.isRegularFile(nested.resolve(AGENT_MAIN_SCRIPT)) ? nested : null;
    }

    /**
     * Mirrors the way node resolves a module: look into the {@code node_modules} of the package, then into the one of
     * each parent directory. Checking the package's own {@code node_modules} only would reject a valid installation
     * whose dependencies npm hoisted to the installation prefix.
     */
    private static boolean canResolveDependency(Path packageRoot, String dependency) {
        for (Path directory = packageRoot.toAbsolutePath(); directory != null; directory = directory.getParent()) {
            if (Files.isDirectory(directory.resolve(NODE_MODULES_DIRECTORY_NAME).resolve(dependency))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Installs the agent into the workspace, once per version. The installation is kept between runs: it downloads
     * from the npm registry, which is both slow and the only part of a local execution needing network access.
     */
    private void installWithNpm() throws LocalAgentException {
        String version = configuration.getNodeAgentVersion();
        Path prefix = workspace.getInstalledAgentDirectory(INSTALLED_AGENT_NAME, version);
        logger.info("Installing the Node.js agent {}@{} into {}. This is done once and reused by later executions...",
            NPM_PACKAGE_NAME, version, prefix);

        try {
            Files.createDirectories(prefix);
        } catch (IOException e) {
            throw new LocalAgentException("Error while creating " + prefix, e);
        }

        List<String> command = List.of(OsCommands.NPM, "install", NPM_PACKAGE_NAME + "@" + version,
            "--prefix", prefix.toAbsolutePath().toString(),
            "--omit=dev", "--no-audit", "--no-fund");
        try {
            OsCommands.Result result = OsCommands.run(command, prefix, NPM_INSTALL_TIMEOUT_MINUTES * 60_000L);
            if (result.exitCode() != 0) {
                // Leaving a half installed directory behind would make the next run believe the agent is there
                deleteQuietly(prefix);
                throw new LocalAgentException("Unable to install the Node.js agent " + NPM_PACKAGE_NAME + "@" + version
                    + " (npm exited with " + result.exitCode() + "). Check that this machine can reach the npm registry,"
                    + " or point --localAgentNode at an existing installation." + System.lineSeparator() + result.output());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            deleteQuietly(prefix);
            throw new LocalAgentException("Error while installing the Node.js agent with npm", e);
        }
    }

    private static void deleteQuietly(Path directory) {
        try {
            FileUtils.deleteDirectory(directory.toFile());
        } catch (IOException e) {
            logger.warn("Failed to clean up {} after a failed installation", directory, e);
        }
    }

    /**
     * Small helpers around the external commands this provider depends on.
     */
    static class OsCommands {

        static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
        static final String NODE = "node";
        /** npm is a shell script on Windows, and unlike an exe it is not resolved from a bare name. */
        static final String NPM = WINDOWS ? "npm.cmd" : "npm";

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

        static int findFreeLoopbackPort() throws LocalAgentException {
            try (java.net.ServerSocket socket = new java.net.ServerSocket(0, 1,
                java.net.InetAddress.getByName(AgentConfWriter.LOOPBACK_HOST))) {
                return socket.getLocalPort();
            } catch (IOException e) {
                throw new LocalAgentException("Unable to reserve a port for the local Node.js agent", e);
            }
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
