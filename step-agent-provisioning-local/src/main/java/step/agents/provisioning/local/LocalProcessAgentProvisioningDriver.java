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

import ch.exense.commons.io.Poller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.agents.provisioning.AgentPoolProvisioningReport;
import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.core.agents.provisioning.AgentPoolSpec;
import step.core.agents.provisioning.AgentProvisioningReport;
import step.core.agents.provisioning.TokenSelectionCriteriaFilter;
import step.core.agents.provisioning.driver.AgentProvisioningDriver;
import step.core.agents.provisioning.driver.AgentProvisioningDriverConfiguration;
import step.core.agents.provisioning.driver.AgentProvisioningError;
import step.core.agents.provisioning.driver.AgentProvisioningRequest;
import step.core.agents.provisioning.driver.AgentProvisioningStatus;
import step.core.execution.ProvisioningException;
import step.core.plans.agents.configuration.AgentProvisioningConfiguration;
import step.grid.AgentRef;
import step.grid.GridImpl;
import step.grid.TokenWrapper;
import step.grid.agent.AgentTypes;
import step.grid.client.AbstractGridClientImpl;
import step.grid.client.LocalGridClientImpl;
import step.grid.tokenpool.Interest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
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
 * nobody asked for locally. Each agent type is therefore served by a single agent process, configured with
 * {@link LocalAgentProvisioningConfiguration#getMaxTokensPerAgent()} tokens.
 * <p>
 * <b>Agents live as long as this driver.</b> An agent is started the first time an execution needs its type and is
 * then shared by all the executions, which run one request per token in parallel, until {@link #close()}. Restarting
 * the agents for every execution would not only cost their start-up time, it would also throw away what they keep
 * between requests, such as the workspaces the Node.js agent prepares per token. An agent found dead or unreachable
 * by a later execution is replaced.
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
    /**
     * Including the providers which are not available: they are the ones able to tell what is missing for their agent
     * type, which is what an execution requiring it needs to hear.
     */
    private final Map<String, LocalAgentProvider> allProvidersByAgentType;
    private final Map<String, ProvisioningSession> provisioningSessions = new ConcurrentHashMap<>();
    /**
     * The running agents, at most one per agent type, shared by all the executions.
     */
    private final Map<String, StartedAgent> agentsByType = new ConcurrentHashMap<>();
    /**
     * One lock per agent type, so that concurrent executions don't start the same agent twice while the start of an
     * agent doesn't delay the executions requiring another type.
     */
    private final Map<String, Object> agentLocks = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public LocalProcessAgentProvisioningDriver(LocalExecutionGrid grid, LocalAgentWorkspace workspace,
                                               LocalAgentProvisioningConfiguration configuration,
                                               Collection<LocalAgentProvider> providers) {
        this.grid = Objects.requireNonNull(grid, "grid must not be null");
        this.gridClient = grid.getGridClient();
        this.workspace = Objects.requireNonNull(workspace, "workspace must not be null");
        this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
        this.allProvidersByAgentType = Objects.requireNonNull(providers, "providers must not be null").stream()
            .collect(Collectors.toMap(LocalAgentProvider::getAgentType, p -> p));
        this.providersByAgentType = providers.stream()
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

    /**
     * @return what the user has to do for the given agent type to be available on this machine, or {@code null} when
     * this distribution has no provider for it or the provider has nothing to add
     */
    String getInstallationHint(String agentType) {
        LocalAgentProvider provider = allProvidersByAgentType.get(agentType);
        return provider != null ? provider.getInstallationHint() : null;
    }

    static String agentPoolName(String agentType) {
        return AGENT_POOL_NAME_PREFIX + agentType;
    }

    /**
     * A manual configuration names the pools of the Step instance the plan normally runs on ({@code windows-medium},
     * {@code linux-large}, ...). Those names mean nothing here and cannot be mapped: they describe machines of an
     * infrastructure, whereas a local execution has one machine and the agents this driver can start on it. The number
     * of agents they ask for is just as meaningless, being a number of machines.
     * <p>
     * The same goes for a plan configuring no agent pool at all ({@code agents: []}), which disables the provisioning
     * to run on the permanent agents of a Step instance: there is no such agent here.
     * <p>
     * What the requirement is turned into is therefore one agent per agent type the token forecasting found the plan
     * needs, each with as many tokens as a local agent is allowed to have: the plan asked not to be sized
     * automatically, so it is given everything the local execution can offer.
     */
    @Override
    public List<AgentPoolRequirementSpec> resolveConfiguredAgentPools(AgentProvisioningConfiguration agentProvisioningConfiguration,
                                                                      List<AgentPoolRequirementSpec> forecasted,
                                                                      Set<Map<String, Interest>> criteriaWithoutMatch) {
        Objects.requireNonNull(agentProvisioningConfiguration, "agentProvisioningConfiguration must not be null");
        if (agentProvisioningConfiguration.enableAutomaticTokenNumberCalculation()) {
            throw new IllegalArgumentException("Resolve agent pools should not be invoked when automatic calculation is enabled.");
        }
        Objects.requireNonNull(forecasted, "forecasted must not be null");
        Objects.requireNonNull(criteriaWithoutMatch, "criteriaWithoutMatch must not be null");
        if (!criteriaWithoutMatch.isEmpty()) {
            throw new ProvisioningException(getUnmatchedCriteriaMessage(criteriaWithoutMatch));
        }
        int maxTokensPerAgent = configuration.getMaxTokensPerAgent();
        List<AgentPoolRequirementSpec> requiredAgentPools = forecasted.stream()
            .map(requirement -> requirement.agentPoolTemplateName)
            .distinct()
            .map(poolName -> new AgentPoolRequirementSpec(poolName, maxTokensPerAgent))
            .collect(Collectors.toList());

        String planConfigurationMessage = agentProvisioningConfiguration.enableAgentProvisioning() ?
            "This plan configures its agent pools manually" :
            "This plan disables the agent provisioning";
        logger.info("{}, this is ignored for local execution, agents are still started automatically.", planConfigurationMessage);
        return requiredAgentPools;
    }

    @Override
    public boolean alwaysProvisionAgents() {
        return true;
    }

    /**
     * Typically reports a keyword of a language whose agent this machine has no installation of.
     */
    @Override
    public String getUnmatchedCriteriaMessage(Set<Map<String, Interest>> criteriaWithoutMatch) {
        return unavailableAgentsMessage(criteriaWithoutMatch, this::getInstallationHint);
    }

    /**
     * Turns the criteria no local agent pool matched into an error naming the agent types and, when their provider has
     * one, what to do about them: the criteria as they are collected ({@code [{$agenttype=dotnet}]}) do say what is
     * missing, but not that the .NET agent is the user's to install, nor how it is told where it is.
     */
    static String unavailableAgentsMessage(Set<Map<String, Interest>> criteriaWithoutMatch,
                                           Function<String, String> installationHints) {
        List<String> agentTypes = criteriaWithoutMatch.stream()
            .map(criteria -> criteria.get(AgentTypes.AGENT_TYPE_KEY))
            .filter(Objects::nonNull)
            .map(Interest::getSelectionPattern)
            .filter(Objects::nonNull)
            .map(Pattern::pattern)
            .distinct()
            .collect(Collectors.toList());
        if (agentTypes.isEmpty()) {
            // Criteria which cannot be read as an agent type, reported as they were collected
            return "This plan requires agents which are not available for local execution: " + criteriaWithoutMatch;
        }
        StringBuilder message = new StringBuilder("This plan requires agent types which are not available for local"
            + " execution: " + String.join(", ", agentTypes) + ".");
        agentTypes.stream().map(installationHints).filter(Objects::nonNull)
            .forEach(hint -> message.append(" ").append(hint));
        return message.toString();
    }

    /**
     * @return a filter reducing the token selection criteria of the keywords to what a local execution can honour
     */
    @Override
    public TokenSelectionCriteriaFilter createTokenSelectionCriteriaFilter() {
        return new LocalTokenSelectionCriteriaFilter();
    }

    /**
     * @return a directory of the local agent workspace: the agents are started from this application, whose class path
     * they do not share
     */
    @Override
    public Path getLocalLibrariesDirectory() {
        return workspace.getInstalledLibrariesDirectory();
    }

    @Override
    public String initializeTokenProvisioningRequest(AgentProvisioningRequest request) {
        ProvisioningSession session = new ProvisioningSession(request);
        provisioningSessions.put(session.provisioningId, session);
        return session.provisioningId;
    }

    @Override
    public AgentProvisioningStatus executeTokenProvisioningRequest(String provisioningRequestId) {
        ProvisioningSession session = requireSession(provisioningRequestId);
        AgentProvisioningStatus status = session.status;
        AgentProvisioningReport report = new AgentProvisioningReport();
        status.provisioningReport = report;

        // The number of tokens per type only matters to warn about an execution needing more than an agent provides:
        // the agents are shared by all executions and always started with the maximum number of tokens.
        Set<String> agentTypes = calculateRequiredTokensByAgentType(session.request.agentPoolRequirementSpecs).keySet();
        status.statusDescription = "Preparing " + agentTypes.size() + " local agent(s)...";

        try {
            // Sequentially: there are at most as many agents as there are agent types, and starting them one after
            // the other keeps the output of a failing one readable.
            for (String agentType : agentTypes) {
                AgentPoolProvisioningReport poolReport = new AgentPoolProvisioningReport(
                    new AgentPoolRequirementSpec(agentPoolName(agentType), 1));
                report.addAgentPoolReport(poolReport);
                try {
                    ensureAgentRunning(agentType);
                    poolReport.success = true;
                } catch (LocalAgentException e) {
                    logger.error("Error while starting the local {} agent", agentType, e);
                    poolReport.error = new AgentProvisioningError(e.getMessage());
                } finally {
                    poolReport.completed = true;
                }
            }

            List<AgentPoolProvisioningReport> failedPools = report.pools.stream().filter(p -> !p.success).toList();
            if (!failedPools.isEmpty()) {
                String message = failedPools.stream().map(p -> p.error.errorMessage).collect(Collectors.joining("; "));
                status.error = new AgentProvisioningError(message);
                // The agents which are running are left as they are: they are shared with the other executions.
                throw new ProvisioningException(message);
            }
            status.statusDescription = agentTypes.size() + " local agent(s) ready";
        } finally {
            status.completed = true;
        }
        return status;
    }

    /**
     * Returns the running agent of the given type, starting it if there is none yet or if the previous one is no
     * longer usable.
     */
    private void ensureAgentRunning(String agentType) throws LocalAgentException {
        Object lock = agentLocks.computeIfAbsent(agentType, t -> new Object());
        synchronized (lock) {
            if (closed) {
                throw new LocalAgentException("The local agent provisioning has been closed");
            }
            StartedAgent existing = agentsByType.get(agentType);
            if (existing != null) {
                if (isUsable(existing)) {
                    logger.debug("Reusing the running local {} agent", agentType);
                    return;
                }
                logger.warn("The local {} agent is no longer reachable and will be restarted. Last output of the agent:{}{}",
                    agentType, System.lineSeparator(), String.join(System.lineSeparator(), existing.process.getLastOutputLines()));
                agentsByType.remove(agentType);
                discardAgent(existing);
            }

            StartedAgent startedAgent = startAgent(agentType, configuration.getMaxTokensPerAgent());
            if (closed) {
                // Closed while the agent was starting: close() could not see it
                stopAgent(startedAgent);
                throw new LocalAgentException("The local agent provisioning has been closed");
            }
            agentsByType.put(agentType, startedAgent);
        }
    }

    private boolean isUsable(StartedAgent agent) {
        if (!agent.process.isAlive() || agent.agentRef == null) {
            return false;
        }
        try {
            gridClient.pingAgent(agent.agentRef);
            return true;
        } catch (AbstractGridClientImpl.AgentCommunicationException e) {
            return false;
        }
    }

    /**
     * Folds the forecast, expressed in numbers of agents per pool, into a number of tokens per agent type.
     */
    // Package private for the sake of the tests, which cover the sizing without starting any agent
    Map<String, Integer> calculateRequiredTokensByAgentType(List<AgentPoolRequirementSpec> requirementSpecs) {
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
                logger.warn("This execution requires {} {} tokens, more than the {} a local agent provides: parallel "
                        + "steps will queue instead of running side by side. Raise {} if this machine can take more.",
                    tokens, agentType, maxTokens,
                    LocalAgentProvisioningConfiguration.optionHint(LocalAgentProvisioningConfiguration.OPTION_MAX_TOKENS, "<count>"));
                return maxTokens;
            }
            // An agent without tokens would never become usable, and the execution would wait for it until it times
            // out. Requirements are normally at least one agent, but a manual configuration can ask for none.
            return Math.max(tokens, 1);
        });
        return tokensByAgentType;
    }

    private StartedAgent startAgent(String agentType, int numberOfTokens) throws LocalAgentException {
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
        String tokenPartition = UUID.randomUUID().toString();
        // An attribute only, with no selection pattern: the agent is shared, every execution may select its tokens
        LocalAgentStartContext startContext = new LocalAgentStartContext(grid.getGridUrl(), workingDirectory,
            numberOfTokens, Map.of(TOKEN_ATTRIBUTE_PARTITION, tokenPartition), Map.of(), grid.getSecurity());
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
            // Selecting on the partition of the agent just started, never on the tokens of a previous one of the same
            // type which the grid may still list
            return gridClient.getTokenHandle(
                Map.of(),
                Map.of(AgentTypes.AGENT_TYPE_KEY, new Interest(Pattern.compile(Pattern.quote(agentType)), true),
                    TOKEN_ATTRIBUTE_PARTITION, new Interest(Pattern.compile(Pattern.quote(tokenPartition)), true)),
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

    /**
     * Only forgets the request: the agents keep running for the next executions and are stopped by {@link #close()}.
     */
    @Override
    public void deprovisionTokens(String provisioningRequestId) {
        provisioningSessions.remove(provisioningRequestId);
    }

    private void stopAgent(StartedAgent startedAgent) {
        // Asking the agent to shut down itself rather than killing the process: it is the only graceful path on
        // Windows, where Process.destroy() terminates the process without running its shutdown hook.
        boolean shutdownRequested = false;
        if (startedAgent.agentRef != null && startedAgent.process.isAlive()) {
            try {
                gridClient.shutdownAgent(startedAgent.agentRef);
                shutdownRequested = true;
            } catch (Exception e) {
                logger.debug("Unable to shut down {} through the grid. It will be terminated.", startedAgent.process.getName(), e);
            }
        }
        // An agent which was never asked to shut down, or which refused the request because it exposes no such
        // service, has no reason to terminate on its own: waiting for it only delays the shutdown by the whole
        // timeout. The Node.js agent is in that case today, it has no /shutdown service.
        startedAgent.process.stop(shutdownRequested ? configuration.getAgentShutdownTimeout().toMillis() : 0);
    }

    /**
     * Stops an agent which is no longer usable and removes its tokens from the grid, which would otherwise keep
     * offering them to the executions until their registration expires.
     */
    private void discardAgent(StartedAgent startedAgent) {
        stopAgent(startedAgent);
        if (startedAgent.agentRef == null) {
            return;
        }
        String agentId = startedAgent.agentRef.getAgentId();

        GridImpl gridImpl = grid.getGrid();
        for (TokenWrapper token : gridImpl.getTokens()) {
            if (token.getAgent() != null && Objects.equals(token.getAgent().getAgentId(), agentId)) {
                try {
                    gridImpl.invalidateToken(token.getID());
                } catch (Exception e) {
                    logger.warn("Unable to remove the token {} of the stopped local agent {} from the grid", token.getID(), agentId, e);
                }
            }
        }
    }

    /**
     * Local execution has no remote agent pools: everything runs on this machine.
     */
    @Override
    public void registerRemoteAgentPoolSpecs(Set<AgentPoolSpec> agentPoolSpecs) {
        // Intentionally empty
    }

    /**
     * Stops the agents started by this driver, which run for its whole lifetime.
     */
    @Override
    public void close() {
        closed = true;
        for (Map.Entry<String, Object> entry : agentLocks.entrySet()) {
            // Waits for an agent being started to be either registered, and stopped here, or stopped by its starter
            synchronized (entry.getValue()) {
                StartedAgent startedAgent = agentsByType.remove(entry.getKey());
                if (startedAgent != null) {
                    stopAgent(startedAgent);
                }
            }
        }
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

        ProvisioningSession(AgentProvisioningRequest request) {
            this.request = request;
        }
    }

    /**
     * A running agent. Its tokens carry a partition attribute unique to this process, which is how its start-up
     * selects them rather than the tokens of a previous agent of the same type the grid may still list.
     */
    private static class StartedAgent {
        final LocalAgentProcess process;
        AgentRef agentRef;

        StartedAgent(LocalAgentProcess process) {
            this.process = process;
        }
    }
}
