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

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.agents.AgentTypeConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Starts the Node.js agent as a separate process.
 * <p>
 * The agent is not shipped with the CLI: it is a set of JavaScript files, and since it needs a Node.js runtime on the
 * machine anyway, embedding it would only save an npm install while making every CLI carry it. It is instead taken
 * from an existing installation - the one the user points at, or a global {@code npm install -g step-node-agent} -
 * and, failing that, installed with npm into the workspace on first use and reused afterwards.
 * <p>
 * All three are started through the command npm generates for the package, and nothing here reads the inside of an
 * installation: a project the user points {@code --localAgentNode} at is one where {@code npm install} was run, which
 * is the case whether the agent came from the registry or from a {@code .tgz} handed to them.
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
    private static final String NODE_MODULES_DIRECTORY_NAME = "node_modules";
    private static final String BIN_DIRECTORY_NAME = ".bin";
    private static final String PACKAGE_JSON_FILE_NAME = "package.json";
    /**
     * Written before installing, so that npm installs into the directory of this CLI rather than into whichever
     * project happens to sit above it: npm looks for the nearest {@code package.json} up the directory tree, and a
     * workspace placed inside a JavaScript project would otherwise be installed into that project.
     */
    private static final String PACKAGE_JSON_CONTENT =
        "{\n  \"name\": \"step-cli-local-node-agent\",\n  \"version\": \"1.0.0\",\n  \"private\": true\n}\n";
    private static final int NPM_INSTALL_TIMEOUT_MINUTES = 10;
    private static final long NPM_LIST_TIMEOUT_MS = 30_000;

    private final LocalAgentProvisioningConfiguration configuration;
    private final LocalAgentWorkspace workspace;
    private final AgentConfWriter agentConfWriter = new AgentConfWriter();

    private Boolean globallyInstalled;

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

    /**
     * Names the runtime which is actually missing: with node installed and npm not, telling a user to install Node.js
     * would send them looking at something they already have.
     */
    @Override
    public String getInstallationHint() {
        if (!OsCommands.isExecutableAvailable(OsCommands.NODE)) {
            return "The Node.js agent runs on Node.js, which is not installed on this machine.";
        }
        return "The Node.js agent is installed with npm, which is not installed on this machine: install it, or point"
            + " --localAgentNode at a directory where " + NPM_PACKAGE_NAME + " has been installed.";
    }

    private boolean isInstalled() {
        return Files.isRegularFile(agentBinary(installedAgentProject()));
    }

    /**
     * @return the project directory this provider installs the agent into, one per version
     */
    private Path installedAgentProject() {
        return workspace.getInstalledAgentDirectory(INSTALLED_AGENT_NAME, configuration.getNodeAgentVersion());
    }

    /**
     * The command npm creates for a package declaring a {@code bin}, in the project it is installed into:
     * {@code <project>/node_modules/.bin/step-node-agent}, {@code .cmd} on Windows.
     * <p>
     * Its presence is the whole definition of "an agent is installed here". Where npm put the package itself, and
     * whether its dependency tree is complete, are npm's business: a package manager is free to lay its store out as
     * it sees fit - npm hoists, pnpm symlinks, Yarn PnP has no {@code node_modules} at all - and the generated
     * command is the one thing all of them provide.
     */
    private static Path agentBinary(Path projectDirectory) {
        return projectDirectory.resolve(NODE_MODULES_DIRECTORY_NAME).resolve(BIN_DIRECTORY_NAME)
            .resolve(OsCommands.STEP_NODE_AGENT).toAbsolutePath();
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
            if (command.get(0).equals(OsCommands.STEP_NODE_AGENT)) {
                // npm reported the agent as globally installed, so the command it created is not on the PATH of this
                // process. Nothing this provider can do about it, but the user can.
                throw new LocalAgentException("The globally installed Node.js agent could not be started: no "
                    + OsCommands.STEP_NODE_AGENT + " command was found. Point --localAgentNode at the installation"
                    + " directory of the agent instead.", e);
            }
            throw new LocalAgentException("Error while starting the local Node.js agent", e);
        }
        return new LocalAgentProcess("Local " + getDisplayName() + " agent", process, runDirectory,
            configuration.isVerbose());
    }

    /**
     * @return the command starting the agent, without its arguments. Either the command a global installation puts on
     * the PATH, or the one npm generated in the project the agent is installed in.
     */
    // Package private for the sake of the tests, which cover the resolution without starting anything
    List<String> resolveAgentCommand() throws LocalAgentException {
        Path configured = configuration.getNodeAgentPath();
        if (configured != null) {
            return List.of(validateConfiguredAgent(configured).toString());
        }
        // A globally installed agent comes before the one installed in the workspace: installing it is an explicit act
        // of the user, who then expects it to be the agent that runs, and it spares the CLI an installation of its
        // own. Its version is whatever was installed, which is deliberate: overriding a global installation with the
        // version of this CLI is not for the CLI to decide.
        if (isGloballyInstalled()) {
            return List.of(OsCommands.STEP_NODE_AGENT);
        }
        return List.of(installAgentIfNeeded().toString());
    }

    /**
     * Checks the project the user pointed at up front, rather than letting the process fail: a missing agent is
     * otherwise reported as a start-up timeout, with the actual cause buried in the output of a process that died
     * seconds earlier.
     *
     * @return the command starting the configured agent
     */
    private static Path validateConfiguredAgent(Path configured) throws LocalAgentException {
        Path binary = agentBinary(configured);
        if (!Files.isRegularFile(binary)) {
            throw new LocalAgentException("No Node.js agent is installed in " + configured + ": " + binary
                + " does not exist. Install the agent into that directory, for instance with 'npm install ./"
                + NPM_PACKAGE_NAME + "-<version>.tgz', and point --localAgentNode at it.");
        }
        return binary;
    }

    /**
     * @return the command starting the agent installed in the workspace, installed here on the first local execution
     * using a Node.js keyword with this CLI version
     */
    private Path installAgentIfNeeded() throws LocalAgentException {
        Path project = installedAgentProject();
        Path binary = agentBinary(project);
        if (Files.isRegularFile(binary)) {
            logger.debug("Using the Node.js agent already installed in {}", project);
            return binary;
        }
        installWithNpm(project);
        if (!Files.isRegularFile(binary)) {
            throw new LocalAgentException("npm reported a successful installation of " + NPM_PACKAGE_NAME
                + " but created no " + OsCommands.STEP_NODE_AGENT + " command in " + binary.getParent());
        }
        return binary;
    }

    /**
     * Reports whether the agent is installed globally, i.e. whether {@code npm install -g step-node-agent} was run on
     * this machine. Such an installation puts the {@code step-node-agent} command on the PATH, which is all that is
     * needed to start it: where npm put the package, and whether that package looks runnable, are questions this
     * provider does not have to answer.
     * <p>
     * Asked at most once: it starts an npm process, and a global installation appearing while the CLI runs is not a
     * case worth paying that on every provisioning for.
     */
    private synchronized boolean isGloballyInstalled() {
        if (globallyInstalled == null) {
            globallyInstalled = lookupGlobalInstallation();
        }
        return globallyInstalled;
    }

    // Package private for the sake of the tests, which must not depend on what the build machine happens to have installed
    boolean lookupGlobalInstallation() {
        if (!OsCommands.isExecutableAvailable(OsCommands.NPM)) {
            return false;
        }
        try {
            OsCommands.Result result = OsCommands.run(List.of(OsCommands.NPM, "list", "-g", NPM_PACKAGE_NAME,
                "--depth=0", "--no-progress"), null, NPM_LIST_TIMEOUT_MS);
            // The exit code is deliberately ignored: npm reports a package it was asked about but did not find as an
            // error, as it does any complaint about the global tree. The listed package itself is the answer.
            String listed = result.output().lines().map(String::trim)
                .filter(line -> line.contains(NPM_PACKAGE_NAME + "@"))
                .findFirst().orElse(null);
            if (listed == null) {
                logger.debug("No globally installed Node.js agent found");
                return false;
            }
            // The line carries the version, which is worth having in the logs: it is the user's, not the CLI's
            logger.debug("Using the globally installed Node.js agent, as listed by npm: {}", listed);
            return true;
        } catch (IOException e) {
            logger.debug("Unable to list the globally installed npm packages", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    /**
     * Installs the agent into the workspace, once per version. The installation is kept between runs: it downloads
     * from the npm registry, which is both slow and the only part of a local execution needing network access.
     */
    private void installWithNpm(Path project) throws LocalAgentException {
        String version = configuration.getNodeAgentVersion();
        logger.info("Installing the Node.js agent {}@{} into {}. This is done once and reused by later executions...",
            NPM_PACKAGE_NAME, version, project);

        try {
            Files.createDirectories(project);
            Files.writeString(project.resolve(PACKAGE_JSON_FILE_NAME), PACKAGE_JSON_CONTENT);
        } catch (IOException e) {
            throw new LocalAgentException("Error while preparing the installation directory " + project, e);
        }

        // A plain local install, run in the directory it installs into: the same command a user runs in their own
        // project, producing the same layout, which is the only one this provider knows how to start.
        List<String> command = List.of(OsCommands.NPM, "install", NPM_PACKAGE_NAME + "@" + version,
            "--omit=dev", "--no-audit", "--no-fund");
        try {
            OsCommands.Result result = OsCommands.run(command, project, NPM_INSTALL_TIMEOUT_MINUTES * 60_000L);
            if (result.exitCode() != 0) {
                // Leaving a half installed directory behind would make the next run believe the agent is there
                deleteQuietly(project);
                throw new LocalAgentException("Unable to install the Node.js agent " + NPM_PACKAGE_NAME + "@" + version
                    + " (npm exited with " + result.exitCode() + "). Check that this machine can reach the npm registry,"
                    + " or point --localAgentNode at an existing installation." + System.lineSeparator() + result.output());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            deleteQuietly(project);
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
        /**
         * npm is a shell script on Windows, and unlike an exe it is not resolved from a bare name.
         */
        static final String NPM = WINDOWS ? "npm.cmd" : "npm";
        /**
         * The command npm generates for the agent package: on the PATH for a global installation, in
         * {@code node_modules/.bin} for a local one. Like npm, it is a script rather than an exe: a shell prompt
         * resolves it from the bare name through PATHEXT, whereas the CreateProcess call behind {@link ProcessBuilder}
         * only ever appends {@code .exe} and needs the extension spelled out.
         */
        static final String STEP_NODE_AGENT = WINDOWS ? NPM_PACKAGE_NAME + ".cmd" : NPM_PACKAGE_NAME;

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
