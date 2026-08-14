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

import ch.exense.commons.io.Poller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.agents.provisioning.AgentPoolProvisioningReport;
import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.core.agents.provisioning.AgentPoolSpec;
import step.core.agents.provisioning.AgentProvisioningReport;
import step.core.agents.provisioning.driver.AgentProvisioningDriver;
import step.core.agents.provisioning.driver.AgentProvisioningDriverConfiguration;
import step.core.agents.provisioning.driver.AgentProvisioningError;
import step.core.agents.provisioning.driver.AgentProvisioningRequest;
import step.core.agents.provisioning.driver.AgentProvisioningStatus;
import step.core.execution.ProvisioningException;
import step.grid.AgentRef;
import step.grid.TokenWrapper;
import step.grid.agent.AgentTypes;
import step.grid.client.AbstractGridClientImpl;
import step.grid.client.LocalGridClientImpl;
import step.grid.tokenpool.Interest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static step.core.agents.provisioning.AgentPoolConstants.TOKEN_ATTRIBUTE_PARTITION;

/**
 * Provisions agents for a local execution by starting them as separate processes on the developer machine.
 * <p>
 * It forks a process and waits for it to join the embedded grid. Everything downstream — token
 * selection, keyword routing by {@link AgentTypes#AGENT_TYPE_KEY}, file transfer — is then identical to a platform
 * execution, which is the point: what runs locally is the real agent, not an approximation of it.
 * <p>
 * <b>One process per agent type.</b> The forecast is expressed in agents of a given pool, but a developer machine is
 * not a cluster: starting one JVM per forecast agent would trade a lot of memory and start-up time for an isolation
 * nobody asked for locally. The tokens forecast for a type are therefore served by a single agent process configured
 * with that many tokens, capped by {@link LocalAgentProvisioningConfiguration#getMaxTokensPerAgent()}.
 * <p>
 * <b>One token per pool agent.</b> The local pools are declared with a single token each, which is what makes the
 * agents be sized on what the execution actually needs. The forecasting turns a number of required tokens into a
 * number of agents of a pool - {@code ceil(tokens / pool.numberOfTokens)} - and only that number reaches a driver:
 * with a pool of ten tokens, everything between one and ten tokens arrives here as "one agent" and would be served
 * by ten. Declaring the granularity of the pool as one token is not a trick, it states what a local agent is: unlike
 * a pool of machines, its size is a number written into its configuration file.
 */
public class LocalProcessAgentProvisioningDriver implements AgentProvisioningDriver {

    private static final Logger logger = LoggerFactory.getLogger(LocalProcessAgentProvisioningDriver.class);
    private static final String AGENT_POOL_NAME_PREFIX = "local-";
    /**
     * The number of tokens an agent of a local pool provides. See the class documentation: it is what lets the
     * forecast reach this driver as a number of tokens rather than as a number of agents.
     */
    private static final int TOKENS_PER_POOL_AGENT = 1;

    private final LocalGridClientImpl gridClient;
    private final LocalExecutionGrid grid;
    private final LocalAgentWorkspace workspace;
    private final LocalAgentProvisioningConfiguration configuration;
    private final Map<String, LocalAgentProvider> providersByAgentType;
    private final Map<String, ProvisioningSession> provisioningSessions = new ConcurrentHashMap<>();

    public LocalProcessAgentProvisioningDriver(LocalExecutionGrid grid, LocalAgentWorkspace workspace,
                                               LocalAgentProvisioningConfiguration configuration,
                                               Collection<LocalAgentProvider> providers) {
        this.grid = Objects.requireNonNull(grid, "grid must not be null");
        this.gridClient = grid.getGridClient();
        this.workspace = Objects.requireNonNull(workspace, "workspace must not be null");
        this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
        this.providersByAgentType = Objects.requireNonNull(providers, "providers must not be null").stream()
            .filter(provider -> {
                boolean available = provider.isAvailable();
                if (!available) {
                    logger.debug("The {} agent is not available in this distribution and won't be offered for local execution.",
                        provider.getDisplayName());
                }
                return available;
            })
            .collect(Collectors.toMap(LocalAgentProvider::getAgentType, p -> p));
        logger.info("Local execution supports the following agent types: {}", providersByAgentType.isEmpty() ?
            "<none>" : providersByAgentType.values().stream().map(LocalAgentProvider::getDisplayName).collect(Collectors.joining(", ")));
    }

    @Override
    public AgentProvisioningDriverConfiguration getConfiguration() {
        return new AgentProvisioningDriverConfiguration(getAvailableAgentPoolSpecs());
    }

    private Set<AgentPoolSpec> getAvailableAgentPoolSpecs() {
        return providersByAgentType.values().stream()
            .map(provider -> new AgentPoolSpec(
                agentPoolName(provider.getAgentType()),
                provider.getDisplayName() + " (local)",
                Map.of(AgentTypes.AGENT_TYPE_KEY, provider.getAgentType()),
                TOKENS_PER_POOL_AGENT,
                Set.of()))
            .collect(Collectors.toSet());
    }

    /**
     * @return the agent types this driver is able to start on this machine
     */
    public Set<String> getAvailableAgentTypes() {
        return Set.copyOf(providersByAgentType.keySet());
    }

    static String agentPoolName(String agentType) {
        return AGENT_POOL_NAME_PREFIX + agentType;
    }

    @Override
    public String initializeTokenProvisioningRequest(AgentProvisioningRequest request) {
        ProvisioningSession session = new ProvisioningSession(request);
        provisioningSessions.put(session.provisioningId, session);
        return session.provisioningId;
    }

    @Override
    public AgentProvisioningStatus executeTokenProvisioningRequest(String provisioningRequestId) throws Exception {
        ProvisioningSession session = requireSession(provisioningRequestId);
        AgentProvisioningStatus status = session.status;
        AgentProvisioningReport report = new AgentProvisioningReport();
        status.provisioningReport = report;

        Map<String, Integer> tokensByAgentType = calculateTokensByAgentType(session.request.agentPoolRequirementSpecs);
        status.statusDescription = "Starting " + tokensByAgentType.size() + " local agent(s)...";

        try {
            // Sequentially: there are at most as many agents as there are agent types, and starting them one after
            // the other keeps the output of a failing one readable.
            for (Map.Entry<String, Integer> entry : tokensByAgentType.entrySet()) {
                String agentType = entry.getKey();
                int numberOfTokens = entry.getValue();
                AgentPoolProvisioningReport poolReport = new AgentPoolProvisioningReport(
                    new AgentPoolRequirementSpec(agentPoolName(agentType), 1));
                report.addAgentPoolReport(poolReport);
                try {
                    session.startedAgents.add(startAgent(agentType, numberOfTokens, session.tokenPartition));
                    poolReport.success = true;
                } catch (LocalAgentException e) {
                    logger.error("Error while starting the local {} agent", agentType, e);
                    poolReport.error = new AgentProvisioningError(e.getMessage());
                } finally {
                    poolReport.completed = true;
                }
            }

            List<AgentPoolProvisioningReport> failedPools = report.pools.stream().filter(p -> !p.success).collect(Collectors.toList());
            if (!failedPools.isEmpty()) {
                String message = failedPools.stream().map(p -> p.error.errorMessage).collect(Collectors.joining("; "));
                status.error = new AgentProvisioningError(message);
                // Stop the agents which did start: a partially provisioned execution would fail later on with a far
                // less obvious error.
                stopAgents(session);
                throw new ProvisioningException(message);
            }
            status.statusDescription = "Started " + tokensByAgentType.size() + " local agent(s)";
        } finally {
            status.completed = true;
        }
        return status;
    }

    /**
     * Folds the forecast, expressed in numbers of agents per pool, into a number of tokens per agent type.
     */
    // Package private for the sake of the tests, which cover the sizing without starting any agent
    Map<String, Integer> calculateTokensByAgentType(List<AgentPoolRequirementSpec> requirementSpecs) {
        Map<String, AgentPoolSpec> availablePools = getAvailableAgentPoolSpecs().stream()
            .collect(Collectors.toMap(s -> s.name, s -> s));

        Map<String, Integer> tokensByAgentType = new LinkedHashMap<>();
        for (AgentPoolRequirementSpec requirementSpec : Objects.requireNonNull(requirementSpecs, "The agent pool requirements must not be null")) {
            AgentPoolSpec poolSpec = availablePools.get(requirementSpec.agentPoolTemplateName);
            if (poolSpec == null) {
                throw new ProvisioningException("No local agent available for the agent pool '"
                    + requirementSpec.agentPoolTemplateName + "'. Available pools: " + availablePools.keySet());
            }
            String agentType = poolSpec.attributes.get(AgentTypes.AGENT_TYPE_KEY);
            int requestedTokens = requirementSpec.numberOfAgents * poolSpec.numberOfTokens;
            tokensByAgentType.merge(agentType, requestedTokens, Integer::sum);
        }

        tokensByAgentType.replaceAll((agentType, tokens) -> {
            int maxTokens = configuration.getMaxTokensPerAgent();
            if (tokens > maxTokens) {
                logger.warn("This execution requires {} {} tokens, more than the {} a local agent is allowed to "
                        + "provide. The agent is started with {} tokens: parallel steps will queue instead of running "
                        + "side by side. Raise --localAgentMaxTokens if this machine can take more.",
                    tokens, agentType, maxTokens, maxTokens);
                return maxTokens;
            }
            // An agent without tokens would never become usable, and the execution would wait for it until it times
            // out. Requirements are normally at least one agent, but a manual configuration can ask for none.
            return Math.max(tokens, 1);
        });
        return tokensByAgentType;
    }

    private StartedAgent startAgent(String agentType, int numberOfTokens, String tokenPartition) throws LocalAgentException {
        LocalAgentProvider provider = providersByAgentType.get(agentType);
        if (provider == null) {
            throw new LocalAgentException("No local agent available for the agent type '" + agentType + "'");
        }

        Path workingDirectory;
        try {
            workingDirectory = workspace.createAgentRunDirectory(agentType);
        } catch (IOException e) {
            throw new LocalAgentException("Error while creating the working directory of the local " + agentType + " agent", e);
        }

        logger.info("Starting the local {} agent with {} token(s)...", provider.getDisplayName(), numberOfTokens);
        LocalAgentStartContext startContext = new LocalAgentStartContext(grid.getGridUrl(), workingDirectory,
            numberOfTokens, Map.of(TOKEN_ATTRIBUTE_PARTITION, tokenPartition), grid.getSecurity());
        LocalAgentProcess process = provider.start(startContext);

        StartedAgent startedAgent = new StartedAgent(process);
        try {
            startedAgent.agentRef = waitForTokensToConnect(process, agentType, numberOfTokens, tokenPartition);
        } catch (LocalAgentException e) {
            process.stop(configuration.getAgentShutdownTimeout().toMillis());
            throw e;
        }
        logger.info("The local {} agent is ready.", provider.getDisplayName());
        return startedAgent;
    }

    /**
     * Waits for all the tokens of a freshly started agent to join the grid and for the agent to answer, then returns
     * all the tokens to the grid. Reserving every token is what guarantees the agent is fully up before the execution
     * starts, rather than discovering half way through it that only some of its tokens ever registered.
     *
     * @return the reference of the agent, needed later on to shut it down gracefully
     */
    private AgentRef waitForTokensToConnect(LocalAgentProcess process, String agentType, int numberOfTokens, String tokenPartition)
        throws LocalAgentException {
        long agentStartTimeoutMs = configuration.getAgentStartTimeout().toMillis();
        long start = System.currentTimeMillis();
        List<TokenWrapper> tokens = new ArrayList<>(numberOfTokens);
        AgentRef agentRef = null;
        try {
            for (int i = 0; i < numberOfTokens; i++) {
                TokenWrapper token = selectToken(process, agentType, tokenPartition);
                tokens.add(token);
                agentRef = token.getAgent();
            }
            waitForAgentToBeReachable(process, agentType, agentRef, agentStartTimeoutMs - (System.currentTimeMillis() - start));
        } finally {
            for (TokenWrapper token : tokens) {
                try {
                    gridClient.returnTokenHandle(token.getID());
                } catch (Exception e) {
                    logger.error("Unable to return the token reserved while waiting for the local {} agent to start", agentType, e);
                }
            }
        }
        return agentRef;
    }

    private TokenWrapper selectToken(LocalAgentProcess process, String agentType, String tokenPartition) throws LocalAgentException {
        try {
            return gridClient.getTokenHandle(
                Map.of(TOKEN_ATTRIBUTE_PARTITION, tokenPartition),
                Map.of(AgentTypes.AGENT_TYPE_KEY, new Interest(Pattern.compile(Pattern.quote(agentType)), true)),
                false);
        } catch (Exception e) {
            throw agentStartFailure(process, agentType, "it did not connect to the grid within "
                + configuration.getAgentStartTimeout().toSeconds() + "s", e);
        }
    }

    private void waitForAgentToBeReachable(LocalAgentProcess process, String agentType, AgentRef agentRef, long remainingTimeoutMs)
        throws LocalAgentException {
        long timeout = Math.max(1, remainingTimeoutMs);
        try {
            Poller.waitFor(() -> {
                try {
                    gridClient.pingAgent(agentRef);
                    return true;
                } catch (AbstractGridClientImpl.AgentCommunicationException e) {
                    return false;
                }
            }, timeout);
        } catch (TimeoutException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw agentStartFailure(process, agentType, "it registered to the grid but could not be reached on "
                + agentRef.getAgentUrl() + " within " + timeout + "ms", e);
        }
    }

    /**
     * Builds the failure of an agent that never became usable, quoting what the agent printed. Without it the only
     * symptom would be a timeout, which says nothing about a missing library, a port conflict or an unreadable
     * configuration.
     */
    private LocalAgentException agentStartFailure(LocalAgentProcess process, String agentType, String reason, Exception cause) {
        StringBuilder message = new StringBuilder("The local ").append(agentType).append(" agent failed to start: ").append(reason).append(".");
        if (!process.isAlive()) {
            message.append(" The agent process has terminated.");
        }
        List<String> output = process.getLastOutputLines();
        if (!output.isEmpty()) {
            message.append(" Last output of the agent:").append(System.lineSeparator())
                .append(String.join(System.lineSeparator(), output));
        }
        return new LocalAgentException(message.toString(), cause);
    }

    @Override
    public AgentProvisioningStatus getTokenProvisioningStatus(String provisioningRequestId) {
        ProvisioningSession session = provisioningSessions.get(provisioningRequestId);
        return session != null ? session.status : null;
    }

    @Override
    public void deprovisionTokens(String provisioningRequestId) {
        ProvisioningSession session = provisioningSessions.remove(provisioningRequestId);
        if (session != null) {
            stopAgents(session);
        }
    }

    private void stopAgents(ProvisioningSession session) {
        long shutdownTimeoutMs = configuration.getAgentShutdownTimeout().toMillis();
        for (StartedAgent startedAgent : session.startedAgents) {
            // Asking the agent to shut down itself rather than killing the process: it is the only graceful path on
            // Windows, where Process.destroy() terminates the process without running its shutdown hook.
            boolean shutdownRequested = false;
            if (startedAgent.agentRef != null) {
                try {
                    gridClient.shutdownAgent(startedAgent.agentRef);
                    shutdownRequested = true;
                } catch (Exception e) {
                    logger.debug("Unable to shut down {} through the grid. It will be terminated.", startedAgent.process.getName(), e);
                }
            }
            // An agent which was never asked to shut down, or which refused the request because it exposes no such
            // service, has no reason to terminate on its own: waiting for it only delays the end of the execution by
            // the whole timeout. The Node.js agent is in that case today, it has no /shutdown service.
            startedAgent.process.stop(shutdownRequested ? shutdownTimeoutMs : 0);
        }
        session.startedAgents.clear();
    }

    /**
     * Local execution has no remote agent pools: everything runs on this machine.
     */
    @Override
    public void registerRemoteAgentPoolSpecs(Set<AgentPoolSpec> agentPoolSpecs) {
        // Intentionally empty
    }

    /**
     * Stops whatever is still running. Normally a no-op, as the agents of an execution are stopped when it is
     * de-provisioned; it covers the executions which never got that far.
     */
    @Override
    public void close() {
        provisioningSessions.values().forEach(this::stopAgents);
        provisioningSessions.clear();
    }

    private ProvisioningSession requireSession(String provisioningRequestId) {
        return Optional.ofNullable(provisioningSessions.get(provisioningRequestId)).orElseThrow(() ->
            new ProvisioningException("No provisioning request found with id " + provisioningRequestId));
    }

    private static class ProvisioningSession {
        final AgentProvisioningRequest request;
        final String provisioningId = UUID.randomUUID().toString();
        final AgentProvisioningStatus status = new AgentProvisioningStatus();
        final Set<StartedAgent> startedAgents = new HashSet<>();
        /**
         * Isolates the tokens of this execution from the ones of any other, exactly as the Kubernetes driver does.
         * The execution id is used when available so that the agent configurations left behind by a crash can be
         * traced back to the execution they belonged to.
         */
        final String tokenPartition;

        ProvisioningSession(AgentProvisioningRequest request) {
            this.request = request;
            this.tokenPartition = Optional.ofNullable(request.executionId).orElseGet(() -> UUID.randomUUID().toString());
        }
    }

    private static class StartedAgent {
        final LocalAgentProcess process;
        AgentRef agentRef;

        StartedAgent(LocalAgentProcess process) {
            this.process = process;
        }
    }
}
