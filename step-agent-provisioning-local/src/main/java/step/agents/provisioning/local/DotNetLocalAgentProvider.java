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
import step.core.agents.AgentTypeConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Starts the .NET agent as a separate process.
 * <p>
 * Unlike the other agents, this one is neither shipped with the CLI nor installable by it: it is a platform specific,
 * self-contained binary distribution, published per runtime identifier. The user therefore provides it, with
 * {@code --localAgentDotNet} or the {@code STEP_DOTNET_AGENT_HOME} environment variable, and the agent type is simply
 * not offered when neither is set.
 * <p>
 * The installation is used <b>in place</b> and never copied: the only thing this provider produces is the
 * configuration of one agent run, written in the run directory of the execution. Everything the agent writes -
 * its working directory, the temporary files of its sessions - is kept there too, so that the installation stays
 * untouched and can be shared, read-only, by concurrent executions.
 */
public class DotNetLocalAgentProvider implements LocalAgentProvider {

    private static final Logger logger = LoggerFactory.getLogger(DotNetLocalAgentProvider.class);
    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public static final String AGENT_HOME_ENV_VAR = "STEP_DOTNET_AGENT_HOME";

    private static final String AGENT_CONF_FILE_NAME = "AgentConf.yaml";
    private static final String BIN_DIRECTORY_NAME = "bin";
    private static final String WORKER_DIRECTORY_NAME = "worker";
    private static final String AGENT_EXECUTABLE_NAME = WINDOWS ? "StepAgent.exe" : "StepAgent";
    private static final String WORKER_EXECUTABLE_NAME = WINDOWS ? "StepAgentWorker.exe" : "StepAgentWorker";

    private final LocalAgentProvisioningConfiguration configuration;
    private final AgentConfWriter agentConfWriter = new AgentConfWriter();

    public DotNetLocalAgentProvider(LocalAgentProvisioningConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String getAgentType() {
        return AgentTypeConstants.AGENT_TYPE_DOTNET;
    }

    @Override
    public String getDisplayName() {
        return ".NET";
    }

    @Override
    public boolean isAvailable() {
        Path configured = configuredAgentPath();
        if (configured == null) {
            return false;
        }
        // Only the presence of the agent is checked here, not the completeness of the installation: an installation
        // which is there but broken has to be reported as an error rather than silently ignored, which it is when
        // the execution actually needs it (see validateInstallation).
        return findBinDirectory(configured) != null;
    }

    /**
     * This agent type is unavailable for two different reasons - nothing configured, or something configured which is
     * not usable - and telling a user who did configure an installation to configure one is worse than saying nothing.
     * The second case is therefore answered by the validation itself, which knows what is wrong with the installation
     * and names it.
     */
    @Override
    public String getInstallationHint() {
        if (configuredAgentPath() == null) {
            return "The .NET agent is a platform specific binary distribution and is not shipped with the CLI: point"
                + " --localAgentDotNet or the " + AGENT_HOME_ENV_VAR + " environment variable at an installed Step .NET"
                + " agent for this platform.";
        }
        try {
            validateInstallation();
            // The installation became usable since it was found unavailable, which leaves nothing to say
            return null;
        } catch (LocalAgentException e) {
            return e.getMessage();
        }
    }

    /**
     * @return the installation the user provided, or {@code null} when there is none. The option wins over the
     * environment variable, which is there to spare the developer from passing the same path at every execution.
     */
    private Path configuredAgentPath() {
        Path configured = configuration.getDotNetAgentPath();
        if (configured != null) {
            return configured;
        }
        String fromEnvironment = System.getenv(AGENT_HOME_ENV_VAR);
        if (fromEnvironment == null || fromEnvironment.isBlank()) {
            return null;
        }
        try {
            return Path.of(fromEnvironment.trim());
        } catch (InvalidPathException e) {
            logger.warn("The {} environment variable is not a valid path: {}", AGENT_HOME_ENV_VAR, fromEnvironment, e);
            return null;
        }
    }

    @Override
    public LocalAgentProcess start(LocalAgentStartContext context) throws LocalAgentException {
        Path binDirectory = validateInstallation();

        Path runDirectory = context.getWorkingDirectory();
        // The agent falls back to a fixed port (8098) when none is configured, and resolves the fully qualified name
        // of the machine when no URL is configured. Both are pinned here, as they are for the Node.js agent.
        int agentPort = LocalPorts.findFreeLoopbackPort();
        Path agentConf;
        try {
            agentConf = agentConfWriter.write(runDirectory, AGENT_CONF_FILE_NAME, context, Map.of(
                "agentPort", agentPort,
                "agentUrl", "http://" + AgentConfWriter.LOOPBACK_HOST + ":" + agentPort,
                // Absolute: the agent resolves this directory against its working directory, which is the run
                // directory of this execution and not the directory it is installed in.
                "workerDir", binDirectory.resolve(WORKER_DIRECTORY_NAME).toAbsolutePath().toString(),
                "workerName", WORKER_EXECUTABLE_NAME));
        } catch (IOException e) {
            throw new LocalAgentException("Error while writing the configuration of the local .NET agent", e);
        }

        List<String> command = List.of(binDirectory.resolve(AGENT_EXECUTABLE_NAME).toAbsolutePath().toString(),
            "-config", agentConf.toAbsolutePath().toString());
        logger.debug("Starting the local .NET agent with command: {}", command);
        Process process;
        try {
            process = new ProcessBuilder(command)
                .directory(runDirectory.toFile())
                // The agent waits for a key press after reporting a configuration error. Reading from the null device
                // rather than from a pipe nobody writes to is what lets it terminate, and its error be reported,
                // instead of leaving it hanging until the start-up timeout expires.
                .redirectInput(ProcessBuilder.Redirect.from(nullDevice()))
                .redirectErrorStream(true)
                .start();
        } catch (IOException e) {
            throw new LocalAgentException("Error while starting the local .NET agent " + binDirectory, e);
        }
        return new LocalAgentProcess("Local " + getDisplayName() + " agent", process, runDirectory,
            configuration.isVerbose());
    }

    private static File nullDevice() {
        return new File(WINDOWS ? "NUL" : "/dev/null");
    }

    /**
     * Checks the installation the user pointed at, rather than letting the agent fail on it. Both failure modes below
     * are otherwise reported as a start-up timeout, with the actual cause buried in the output of a process that died
     * seconds earlier.
     *
     * @return the {@code bin} directory of the installation, the one holding the agent executable
     */
    // Package private for the sake of the tests, which cover the layouts a user can point at without starting anything
    Path validateInstallation() throws LocalAgentException {
        Path configured = configuredAgentPath();
        if (configured == null) {
            throw new LocalAgentException("No .NET agent is configured. Point --localAgentDotNet or the "
                + AGENT_HOME_ENV_VAR + " environment variable at an installed Step .NET agent.");
        }
        Path binDirectory = findBinDirectory(configured);
        if (binDirectory == null) {
            throw new LocalAgentException("The configured .NET agent " + configured + " contains no "
                + AGENT_EXECUTABLE_NAME + ", neither directly nor in " + BIN_DIRECTORY_NAME + ". Point"
                + " --localAgentDotNet at an installed Step .NET agent for this platform.");
        }
        Path worker = binDirectory.resolve(WORKER_DIRECTORY_NAME).resolve(WORKER_EXECUTABLE_NAME);
        if (!Files.isRegularFile(worker)) {
            throw new LocalAgentException("The .NET agent in " + binDirectory + " has no worker: " + worker
                + " does not exist. The agent runs its keywords in that process, and an installation without it is"
                + " either incomplete or meant for another platform.");
        }
        makeExecutable(binDirectory.resolve(AGENT_EXECUTABLE_NAME));
        makeExecutable(worker);
        return binDirectory;
    }

    /**
     * Accepts the two layouts a user can reasonably point at: the agent distribution, or the {@code bin} directory
     * inside it.
     *
     * @return the directory holding the agent executable, or {@code null} if it holds none
     */
    private static Path findBinDirectory(Path directory) {
        if (Files.isRegularFile(directory.resolve(AGENT_EXECUTABLE_NAME))) {
            return directory;
        }
        Path binDirectory = directory.resolve(BIN_DIRECTORY_NAME);
        return Files.isRegularFile(binDirectory.resolve(AGENT_EXECUTABLE_NAME)) ? binDirectory : null;
    }

    /**
     * The agent distribution is a zip, which carries no permissions: its start script sets them itself, and this
     * provider starts the agent directly. A failure here is not reported as an error of its own, the process start
     * failing right after with the message of the operating system.
     */
    private static void makeExecutable(Path file) {
        if (WINDOWS || Files.isExecutable(file)) {
            return;
        }
        logger.debug("Making {} executable", file);
        if (!file.toFile().setExecutable(true)) {
            logger.warn("Unable to make {} executable. Run 'chmod +x' on it if the agent fails to start.", file.toAbsolutePath());
        }
    }
}
