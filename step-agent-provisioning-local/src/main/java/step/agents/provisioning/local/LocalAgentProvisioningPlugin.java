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

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.handlers.functions.TokenForecastingContext;
import step.artefacts.handlers.functions.TokenForecastingExecutionPlugin;
import step.artefacts.handlers.functions.TokenSelectionCriteriaFilter;
import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.core.agents.provisioning.driver.AgentProvisioningDriver;
import step.core.agents.provisioning.driver.AgentProvisioningRequest;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.DeprovisioningException;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.ProvisioningException;
import step.core.plans.agents.configuration.AgentProvisioningConfiguration;
import step.core.plans.agents.configuration.AutomaticAgentProvisioningConfiguration;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.FunctionPlugin;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.grid.Grid;
import step.grid.agent.AgentTypes;
import step.grid.client.GridClient;
import step.grid.tokenpool.Interest;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static step.core.plans.agents.configuration.AutomaticAgentProvisioningConfiguration.PlanAgentsPoolAutoMode.auto_detect;

/**
 * Runs the keywords of a local execution on real agents started on the developer machine.
 * <p>
 * This is the local equivalent of what the controller does with its grid and its agent provisioning driver, packed
 * into a single execution engine plugin: it starts an embedded grid, publishes it and a
 * {@link LocalProcessAgentProvisioningDriver} in the engine context so that the function and token forecasting
 * plugins pick them up, and provisions the agents an execution needs when it starts.
 * <p>
 * It is deliberately <b>not</b> auto-discovered: the JUnit runner, where
 * running keywords in the same JVM is a feature rather than a limitation, must keep the in-JVM path.
 * <p>
 * Unlike most plugins it is {@link Closeable}, and has to be closed by whoever built it, after the execution engine it
 * was given to. See {@link #close()}.
 */
// Runs before FunctionPlugin, which builds the function execution service around whichever grid client it finds in
// the context: the grid of the local agents has to be published before that. FunctionPlugin knows nothing about this
// plugin, hence runsBefore rather than a dependency declared on its side.
@Plugin(runsBefore = {FunctionPlugin.class})
@IgnoreDuringAutoDiscovery
public class LocalAgentProvisioningPlugin extends AbstractExecutionEnginePlugin implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(LocalAgentProvisioningPlugin.class);
    private static final String PROVISIONING_REQUEST_ID = "$provisioningRequestId";

    private final LocalAgentProvisioningConfiguration configuration;
    private LocalExecutionGrid grid;
    private LocalProcessAgentProvisioningDriver driver;
    private boolean closed;

    public LocalAgentProvisioningPlugin() {
        this(new LocalAgentProvisioningConfiguration());
    }

    public LocalAgentProvisioningPlugin(LocalAgentProvisioningConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
    }

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
        if (closed) {
            // Would otherwise publish the grid client of a grid which has been stopped, and fail every keyword later
            throw new PluginCriticalException("This local agent provisioning plugin has been closed and cannot be reused");
        }
        if (driver != null) {
            return;
        }
        // Failures here are raised as PluginCriticalException, carrying on would leave the engine without a grid and without a
        // provisioning driver, and every plan would then fail with a misleading "no agent available for local
        // execution" instead of with the actual reason the local execution could not be set up.
        LocalAgentWorkspace workspace;
        try {
            workspace = new LocalAgentWorkspace(configuration.getWorkDirectory());
        } catch (IOException e) {
            throw new PluginCriticalException("Error while creating the working directory of the local agents", e);
        }

        try {
            grid = new LocalExecutionGrid(configuration.getAgentStartTimeout(), workspace);
        } catch (Exception e) {
            throw new PluginCriticalException("Error while starting the local grid", e);
        }

        try {
            driver = new LocalProcessAgentProvisioningDriver(grid, workspace, configuration, List.of(
                new JavaLocalAgentProvider(configuration, workspace),
                new NodeLocalAgentProvider(configuration, workspace),
                new DotNetLocalAgentProvider(configuration)));
        } catch (RuntimeException e) {
            closeQuietly(grid);
            grid = null;
            throw new PluginCriticalException("Error while initializing the local agents", e);
        }

        // Picked up by FunctionPlugin (grid client) and TokenForecastingExecutionPlugin (driver). Both are Closeable
        // and are closed by the context when the execution engine is closed: the grid client releases the class
        // loaders of the local tokens and the driver stops any agent still running. The grid itself is deliberately
        // not registered here, see close().
        context.put(Grid.class, grid.getGrid());
        context.put(GridClient.class, grid.getGridClient());
        context.put(AgentProvisioningDriver.class, driver);

        // Last, and deliberately so: the script engines only concern the keywords of two languages, while an
        // execution without the driver in its context fails on every keyword, with a "no agent type available"
        // which points at everything except the actual cause.
        declareScriptEngineLibraries(context, workspace);
    }

    /**
     * Points {@code plugins.<language>.libs} at the script engine libraries, the way the step.properties of a
     * controller does. Without them a Groovy or JavaScript keyword reaches the agent and fails there with "Unable to
     * find script engine": the engine lives in the CLI, and the agent runs in its own process with its own class path.
     * <p>
     * A value already configured wins, so that an agent can be sent a different Groovy than the one the CLI runs on.
     */
    private static void declareScriptEngineLibraries(ExecutionEngineContext context, LocalAgentWorkspace workspace) {
        Configuration configuration = context.getConfiguration();
        if (configuration == null) {
            configuration = new Configuration();
            context.setConfiguration(configuration);
        }
        ScriptEngineLibraries libraries = new ScriptEngineLibraries(workspace);
        for (ScriptEngineLibraries.ScriptEngine engine : List.of(ScriptEngineLibraries.GROOVY, ScriptEngineLibraries.JAVASCRIPT)) {
            String property = "plugins." + engine.language() + ".libs";
            if (configuration.getProperty(property, null) != null) {
                continue;
            }
            try {
                Path directory = libraries.resolve(engine);
                if (directory != null) {
                    configuration.putProperty(property, directory.toString());
                }
            } catch (Exception e) {
                // Not worth aborting the execution: only the keywords of that language are affected, and they fail
                // with an error of their own naming the missing engine. Every exception is caught, not only the
                // expected one: resolving the engines reads how the application itself is packaged, and the way that
                // fails is not ours to predict.
                logger.warn("The {} keywords will not be executable: unable to provide the script engine to the agents.",
                    engine.language(), e);
            }
        }
    }

    /**
     * Registers the filter reducing the token selection criteria to what a local execution can honour.
     */
    @Override
    public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext context) {
        if (driver != null) {
            context.put(TokenSelectionCriteriaFilter.class, new LocalTokenSelectionCriteriaFilter());
        }
    }

    /**
     * @return the keywords available to this execution, or {@code null} when they cannot be listed
     */
    private static Collection<Function> functionsOf(ExecutionContext context) {
        FunctionAccessor functionAccessor = context.get(FunctionAccessor.class);
        if (functionAccessor == null) {
            return null;
        }
        List<Function> functions = new ArrayList<>();
        functionAccessor.getAll().forEachRemaining(functions::add);
        return functions;
    }

    @Override
    public void provisionRequiredResources(ExecutionContext context) {
        AgentProvisioningConfiguration planAgentConfiguration = Objects.requireNonNullElse(
            context.getPlan().getAgents(), new AutomaticAgentProvisioningConfiguration(auto_detect));

        List<AgentPoolRequirementSpec> requiredAgentPools;
        if (planAgentConfiguration.enableAutomaticTokenNumberCalculation()) {
            TokenForecastingContext tokenForecastingContext = TokenForecastingExecutionPlugin.getTokenForecastingContext(context);
            requiredAgentPools = tokenForecastingContext.getAgentPoolRequirementSpec();
            Set<Map<String, Interest>> criteriaWithoutMatch = tokenForecastingContext.getCriteriaWithoutMatch();
            if (!criteriaWithoutMatch.isEmpty()) {
                // Typically a keyword of a language whose agent this machine has no installation of
                throw new ProvisioningException(unavailableAgentsMessage(criteriaWithoutMatch, driver::getInstallationHint));
            }
        } else {
            List<AgentPoolRequirementSpec> configuredAgentPools = planAgentConfiguration.getAgentPoolRequirementSpecs();
            if (configuredAgentPools == null) {
                throw new ProvisioningException("The agent configuration of the plan returned no agent pool requirement");
            }
            requiredAgentPools = LocalAgentPoolRequirements.forRequiredAgentTypes(functionsOf(context),
                context.get(FunctionTypeRegistry.class), driver.getAvailableAgentTypes(),
                configuration.getMaxTokensPerAgent());
            logger.info("This plan configures its agent pools manually ({}). Those pools are those of a Step "
                    + "instance and do not exist here: one agent of each required type is started with {} tokens "
                    + "instead ({}).",
                configuredAgentPools.stream().map(p -> p.agentPoolTemplateName).collect(Collectors.joining(", ")),
                configuration.getMaxTokensPerAgent(),
                requiredAgentPools.stream().map(p -> p.agentPoolTemplateName).collect(Collectors.joining(", ")));
        }

        if (requiredAgentPools.isEmpty()) {
            logger.debug("This plan requires no agent");
            return;
        }

        AgentProvisioningRequest request = new AgentProvisioningRequest();
        request.executionId = context.getExecutionId();
        request.agentPoolRequirementSpecs = requiredAgentPools;

        String provisioningRequestId = driver.initializeTokenProvisioningRequest(request);
        context.put(PROVISIONING_REQUEST_ID, provisioningRequestId);
        try {
            driver.executeTokenProvisioningRequest(provisioningRequestId);
        } catch (ProvisioningException e) {
            throw e;
        } catch (Exception e) {
            throw new ProvisioningException("Error while starting the local agents", e);
        }
    }

    /**
     * Turns the criteria no local agent pool matched into an error naming the agent types and, when their provider has
     * one, what to do about them: the criteria as they are collected ({@code [{$agenttype=dotnet}]}) do say what is
     * missing, but not that the .NET agent is the user's to install, nor how this CLI is told where it is.
     */
    // Package private for the sake of the tests, which cover the message without running an execution
    // Qualified: step.functions.Function, the keyword, is the Function of this package
    static String unavailableAgentsMessage(Set<Map<String, Interest>> criteriaWithoutMatch,
                                           java.util.function.Function<String, String> installationHints) {
        List<String> agentTypes = criteriaWithoutMatch.stream()
            .map(criteria -> criteria.get(AgentTypes.AGENT_TYPE_KEY))
            .filter(Objects::nonNull)
            .map(Interest::getSelectionPattern)
            .filter(Objects::nonNull)
            .map(Pattern::pattern)
            .distinct()
            .collect(Collectors.toList());
        if (agentTypes.isEmpty()) {
            // Criteria this plugin cannot read as an agent type, reported as they were collected
            return "This plan requires agents which are not available for local execution: " + criteriaWithoutMatch;
        }
        StringBuilder message = new StringBuilder("This plan requires agent types which are not available for local"
            + " execution: " + String.join(", ", agentTypes) + ".");
        agentTypes.stream().map(installationHints).filter(Objects::nonNull)
            .forEach(hint -> message.append(" ").append(hint));
        return message.toString();
    }

    @Override
    public void deprovisionRequiredResources(ExecutionContext context) {
        String provisioningRequestId = (String) context.get(PROVISIONING_REQUEST_ID);
        if (provisioningRequestId == null) {
            return;
        }
        context.remove(PROVISIONING_REQUEST_ID);
        try {
            driver.deprovisionTokens(provisioningRequestId);
        } catch (Exception e) {
            throw new DeprovisioningException("Error while stopping the local agents", e);
        }
    }

    /**
     * Stops the local grid and deletes the files its file manager cached.
     * <p>
     * Done here rather than by registering the grid in the execution engine context, because the context closes what
     * it holds in no particular order, while this has to happen <b>last</b>: the local tokens of the grid client load
     * their handlers (the composite handler of every plan calling a composite keyword, for one) with class loaders
     * reading the jars straight out of the file manager directory of this grid. Deleting it before the grid client
     * released them leaves the files open, and Windows refuses to delete an open file.
     * <p>
     * The caller closes this plugin after the execution engine it was given to, typically by declaring it as the first
     * resource of the same try-with-resources.
     */
    @Override
    public void close() throws IOException {
        closed = true;
        if (grid != null) {
            grid.close();
            grid = null;
        }
    }

    private static void closeQuietly(LocalExecutionGrid grid) {
        try {
            grid.close();
        } catch (IOException e) {
            logger.warn("Error while stopping the local grid after a failed initialization", e);
        }
    }
}
