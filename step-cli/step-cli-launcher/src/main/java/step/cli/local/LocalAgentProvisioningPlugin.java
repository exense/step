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
import step.artefacts.handlers.functions.TokenForecastingContext;
import step.artefacts.handlers.functions.TokenForecastingExecutionPlugin;
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
import step.grid.Grid;
import step.grid.client.GridClient;
import step.grid.tokenpool.Interest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
 */
// Runs before FunctionPlugin, which builds the function execution service around whichever grid client it finds in
// the context: the grid of the local agents has to be published before that. FunctionPlugin knows nothing about this
// plugin, hence runsBefore rather than a dependency declared on its side.
@Plugin(runsBefore = {FunctionPlugin.class})
@IgnoreDuringAutoDiscovery
public class LocalAgentProvisioningPlugin extends AbstractExecutionEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(LocalAgentProvisioningPlugin.class);
    private static final String PROVISIONING_REQUEST_ID = "$provisioningRequestId";

    private final LocalAgentProvisioningConfiguration configuration;
    private LocalExecutionGrid grid;
    private LocalProcessAgentProvisioningDriver driver;

    public LocalAgentProvisioningPlugin() {
        this(new LocalAgentProvisioningConfiguration());
    }

    public LocalAgentProvisioningPlugin(LocalAgentProvisioningConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
    }

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
        if (driver != null) {
            return;
        }
        // Failures here are raised as PluginCriticalException, carrying on would leave the engine without a grid and without a
        // provisioning driver, and every plan would then fail with a misleading "no agent available for local
        // execution" instead of with the actual reason the local execution could not be set up.
        try {
            grid = new LocalExecutionGrid(configuration.getAgentStartTimeout());
        } catch (Exception e) {
            throw new PluginCriticalException("Error while starting the local grid", e);
        }

        LocalAgentWorkspace workspace;
        try {
            workspace = new LocalAgentWorkspace(configuration.getWorkDirectory());
        } catch (IOException e) {
            // The grid is not in the context yet at this point, so nothing else would ever stop it
            closeQuietly(grid);
            grid = null;
            throw new PluginCriticalException("Error while creating the working directory of the local agents", e);
        }

        try {
            driver = new LocalProcessAgentProvisioningDriver(grid, workspace, configuration, List.of(
                new JavaLocalAgentProvider(configuration, workspace),
                new NodeLocalAgentProvider(configuration, workspace)));
        } catch (RuntimeException e) {
            closeQuietly(grid);
            grid = null;
            throw new PluginCriticalException("Error while initializing the local agents", e);
        }

        // Picked up by FunctionPlugin (grid client) and TokenForecastingExecutionPlugin (driver). All three are
        // Closeable and are registered in the context, which closes them when the execution engine is closed: the
        // grid client and the grid are shut down, and the driver stops any agent still running.
        context.put(LocalExecutionGrid.class, grid);
        context.put(Grid.class, grid.getGrid());
        context.put(GridClient.class, grid.getGridClient());
        context.put(AgentProvisioningDriver.class, driver);
    }

    @Override
    public void provisionRequiredResources(ExecutionContext context) {
        AgentProvisioningConfiguration planAgentConfiguration = Objects.requireNonNullElse(
            context.getPlan().getAgents(), new AutomaticAgentProvisioningConfiguration(auto_detect));
        if (!planAgentConfiguration.enableAgentProvisioning()) {
            logger.debug("Agent provisioning is disabled for this plan");
            return;
        }

        List<AgentPoolRequirementSpec> requiredAgentPools;
        if (planAgentConfiguration.enableAutomaticTokenNumberCalculation()) {
            TokenForecastingContext tokenForecastingContext = TokenForecastingExecutionPlugin.getTokenForecastingContext(context);
            requiredAgentPools = tokenForecastingContext.getAgentPoolRequirementSpec();
            Set<Map<String, Interest>> criteriaWithoutMatch = tokenForecastingContext.getCriteriaWithoutMatch();
            if (!criteriaWithoutMatch.isEmpty()) {
                // Typically a keyword of a language whose agent this distribution doesn't ship
                throw new ProvisioningException("This plan requires agents which are not available for local execution: "
                    + criteriaWithoutMatch);
            }
        } else {
            requiredAgentPools = planAgentConfiguration.getAgentPoolRequirementSpecs();
            if (requiredAgentPools == null) {
                throw new ProvisioningException("The agent configuration of the plan returned no agent pool requirement");
            }
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

    private static void closeQuietly(LocalExecutionGrid grid) {
        try {
            grid.close();
        } catch (IOException e) {
            logger.warn("Error while stopping the local grid after a failed initialization", e);
        }
    }
}
