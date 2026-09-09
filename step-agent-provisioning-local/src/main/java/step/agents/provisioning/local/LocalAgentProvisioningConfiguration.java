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


import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * The settings of a local execution.
 * <p>
 * Populated from the {@code --localAgent*} options of {@code ap execute}, which means they can equally be set on the
 * command line, in {@code ~/stepcli.properties} or in a file passed with {@code --config}. They are deliberately not
 * system properties: the Windows CLI is a launch4j executable, which offers no practical way of passing {@code -D}.
 * <p>
 * All defaults are meant to be right on a developer machine; none of them normally has to be set.
 */
public class LocalAgentProvisioningConfiguration {

    public static final int DEFAULT_MAX_TOKENS_PER_AGENT = 5;
    public static final int DEFAULT_START_TIMEOUT_SECONDS = 60;
    private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(15);
    /**
     * The floor under which a timeout is not one: no agent process starts, and none acts on a shutdown request, in
     * under a second. It is also the granularity the CLI exposes these timeouts with.
     */
    private static final Duration MINIMUM_TIMEOUT = Duration.ofSeconds(1);

    private Path javaAgentPath;
    private Path nodeAgentPath;
    private Path dotNetAgentPath;
    private Path workDirectory;
    private int maxTokensPerAgent = DEFAULT_MAX_TOKENS_PER_AGENT;
    private Duration agentStartTimeout = Duration.ofSeconds(DEFAULT_START_TIMEOUT_SECONDS);
    private Duration agentShutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;
    private List<String> javaAgentVmArgs = List.of();
    private boolean verbose;
    private boolean debug;

    /**
     * @return the directory of an already installed Java agent to use instead of the one embedded in the CLI, or
     * {@code null} to use the embedded one. Expected to be an unpacked agent distribution, i.e. to contain a
     * {@code lib} directory.
     */
    public Path getJavaAgentPath() {
        return javaAgentPath;
    }

    public LocalAgentProvisioningConfiguration setJavaAgentPath(Path javaAgentPath) {
        this.javaAgentPath = javaAgentPath;
        return this;
    }

    /**
     * @return the directory of an already installed Node.js agent to use, or {@code null} to use a globally installed
     * one and, failing that, install it with npm on first use
     */
    public Path getNodeAgentPath() {
        return nodeAgentPath;
    }

    public LocalAgentProvisioningConfiguration setNodeAgentPath(Path nodeAgentPath) {
        this.nodeAgentPath = nodeAgentPath;
        return this;
    }

    /**
     * @return the directory of an installed .NET agent to use, or {@code null} to fall back to the
     * {@code STEP_DOTNET_AGENT_HOME} environment variable. Unlike the other agents, this one is never installed by the
     * CLI: it is a platform specific binary distribution, which the user provides.
     */
    public Path getDotNetAgentPath() {
        return dotNetAgentPath;
    }

    public LocalAgentProvisioningConfiguration setDotNetAgentPath(Path dotNetAgentPath) {
        this.dotNetAgentPath = dotNetAgentPath;
        return this;
    }

    /**
     * @return the directory the agents are installed and run in, or {@code null} for a directory under the system
     * temporary directory
     */
    public Path getWorkDirectory() {
        return workDirectory;
    }

    public LocalAgentProvisioningConfiguration setWorkDirectory(Path workDirectory) {
        this.workDirectory = workDirectory;
        return this;
    }

    /**
     * The ceiling on the number of tokens a local agent is started with. A token is a thread able to run a keyword,
     * and an agent is otherwise sized on what the execution is forecast to need: this only keeps an unconstrained
     * forecast - a thread group with a high user count, say - from sizing an agent far beyond what a developer
     * machine can run.
     */
    public int getMaxTokensPerAgent() {
        return maxTokensPerAgent;
    }

    public LocalAgentProvisioningConfiguration setMaxTokensPerAgent(int maxTokensPerAgent) {
        if (maxTokensPerAgent < 1) {
            // An agent allowed no token runs no keyword, and is started for nothing
            throw new IllegalArgumentException("maxTokensPerAgent must be at least 1, but was " + maxTokensPerAgent);
        }
        this.maxTokensPerAgent = maxTokensPerAgent;
        return this;
    }

    /**
     * How long to wait for an agent to start and register. Generous by default, because it covers a cold start on a
     * machine which may be busy, and being wrong in this direction only costs time when something is already broken.
     */
    public Duration getAgentStartTimeout() {
        return agentStartTimeout;
    }

    public LocalAgentProvisioningConfiguration setAgentStartTimeout(Duration agentStartTimeout) {
        this.agentStartTimeout = requireUsableTimeout(agentStartTimeout, "agentStartTimeout");
        return this;
    }

    /**
     * @return how long an agent asked to shut down is given to do so before it is destroyed
     */
    public Duration getAgentShutdownTimeout() {
        return agentShutdownTimeout;
    }

    public LocalAgentProvisioningConfiguration setAgentShutdownTimeout(Duration agentShutdownTimeout) {
        this.agentShutdownTimeout = requireUsableTimeout(agentShutdownTimeout, "agentShutdownTimeout");
        return this;
    }

    /**
     * Rejects the timeouts no agent could ever meet, {@code Duration.ZERO} being the one a user may well read as
     * "unlimited": an agent is given a timeout of zero to mean that it was never asked to stop and is to be destroyed
     * right away.
     */
    private static Duration requireUsableTimeout(Duration timeout, String name) {
        Objects.requireNonNull(timeout, name + " must not be null");
        if (timeout.compareTo(MINIMUM_TIMEOUT) < 0) {
            throw new IllegalArgumentException(name + " must be at least " + MINIMUM_TIMEOUT.toSeconds()
                + "s, but was " + timeout);
        }
        return timeout;
    }

    /**
     * @return extra JVM arguments for the Java agent, mainly to attach a debugger or tune the heap while
     * troubleshooting a keyword locally. One argument per element, so that an argument may contain spaces.
     */
    public List<String> getJavaAgentVmArgs() {
        return javaAgentVmArgs;
    }

    public LocalAgentProvisioningConfiguration setJavaAgentVmArgs(List<String> javaAgentVmArgs) {
        this.javaAgentVmArgs = javaAgentVmArgs == null ? List.of() : List.copyOf(javaAgentVmArgs);
        return this;
    }

    /**
     * Whether to print what the agents log.
     * <p>
     * The agents run in their own processes and their output is normally discarded, which keeps the output of a
     * local execution as clean as one running against a controller. It is also the first thing needed to understand
     * anything that goes wrong on an agent, hence this switch.
     */
    public boolean isVerbose() {
        return verbose;
    }

    public LocalAgentProvisioningConfiguration setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    /**
     * Whether the agents log at debug level. Independent from {@link #isVerbose()}, which decides whether their
     * output is shown: the retained lines used to explain a failed start benefit from the level too.
     */
    public boolean isDebug() {
        return debug;
    }

    public LocalAgentProvisioningConfiguration setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }
}
