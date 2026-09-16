/*
 * Copyright (C) 2024, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */
package step.artefacts.handlers.functions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.core.agents.provisioning.AgentProvisioningRestrictions;
import step.core.agents.provisioning.TokenSelectionCriteriaFilter;
import step.core.agents.provisioning.driver.AgentProvisioningDriver;
import step.core.agents.provisioning.driver.AgentProvisioningRequest;
import step.core.agents.provisioning.driver.AgentProvisioningStatus;
import step.core.agents.provisioning.driver.AgentProvisioningStatusAccessor;
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
import step.grid.tokenpool.Interest;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static step.core.agents.provisioning.driver.AgentProvisioningStatus.AGENT_PROVISIONING_STATUS_ID_CUSTOM_FIELD;
import static step.core.plans.agents.configuration.AutomaticAgentProvisioningConfiguration.PlanAgentsPoolAutoMode.auto_detect;

/**
 * This plugin is responsible for the provisioning of agents.
 * It delegates the estimation of the required number of tokens (token forecasting)
 * and provisioning of the agents to the {@link AgentProvisioningDriver} of the execution engine context.
 * <p>
 * It is not auto-discovered: it is added to the execution engine together with the driver, by the Step controller when
 * a driver is configured, and by the CLI and the IDE for local executions.
 */
@Plugin
@IgnoreDuringAutoDiscovery
public class AgentProvisioningExecutionPlugin extends AbstractExecutionEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(AgentProvisioningExecutionPlugin.class);
    public static final String PROVISIONING_REQUEST_ID = "$provisioningRequestId";
    private final AgentProvisioningStatusAccessor agentProvisioningStatusAccessor;
    private AgentProvisioningDriver agentProvisioningDriver;

    public AgentProvisioningExecutionPlugin(AgentProvisioningStatusAccessor agentProvisioningStatusAccessor) {
        this.agentProvisioningStatusAccessor = Objects.requireNonNull(agentProvisioningStatusAccessor, "agentProvisioningStatusAccessor must not be null");
    }

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext executionEngineContext) {
        if (agentProvisioningDriver == null) {
            // The driver is either provided by the parent context (controller) or by a plugin running before this one
            AgentProvisioningDriver parentDriver = parentContext != null ? parentContext.get(AgentProvisioningDriver.class) : null;
            agentProvisioningDriver = parentDriver != null ? parentDriver : executionEngineContext.get(AgentProvisioningDriver.class);
            if (agentProvisioningDriver == null) {
                throw new PluginCriticalException("No agent provisioning driver found in the execution engine context");
            }
            executionEngineContext.put(AgentProvisioningDriver.class, agentProvisioningDriver);
        }
    }

    @Override
    public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext context) {
        TokenSelectionCriteriaFilter tokenSelectionCriteriaFilter = agentProvisioningDriver.createTokenSelectionCriteriaFilter();
        if (tokenSelectionCriteriaFilter != null) {
            context.put(TokenSelectionCriteriaFilter.class, tokenSelectionCriteriaFilter);
        }
    }

    @Override
    public void provisionRequiredResources(ExecutionContext context) {
        AgentProvisioningConfiguration planAgentConfiguration = getPlanAgentPoolConfiguratorOrDefault(context);
        if (planAgentConfiguration.enableAgentProvisioning()) {
            // The token forecasting is always calculated, even if the agent pools are configured manually
            TokenForecastingContext tokenForecastingContext = TokenForecastingExecutionPlugin.getTokenForecastingContext(context);
            List<AgentPoolRequirementSpec> requiredAgentPools;
            if (planAgentConfiguration.enableAutomaticTokenNumberCalculation()) {
                logger.debug("Calculating agent pool requirements...");
                // Get the results of the token forecasting
                requiredAgentPools = tokenForecastingContext.getAgentPoolRequirementSpec();
                Set<Map<String, Interest>> criteriaWithoutMatch = tokenForecastingContext.getCriteriaWithoutMatch();
                if (!criteriaWithoutMatch.isEmpty()) {
                    throw new ProvisioningException(agentProvisioningDriver.getUnmatchedCriteriaMessage(criteriaWithoutMatch));
                }
                logger.info("Calculated agent pool requirements: " + requiredAgentPools);
            } else {
                List<AgentPoolRequirementSpec> configuredAgentPools = planAgentConfiguration.getAgentPoolRequirementSpecs();
                if (configuredAgentPools == null) {
                    throw new ProvisioningException("The method getAgentPoolRequirementSpecs of the plan agent configuration returned null");
                }
                requiredAgentPools = agentProvisioningDriver.resolveConfiguredAgentPools(configuredAgentPools,
                    tokenForecastingContext.getAgentPoolRequirementSpec(), tokenForecastingContext.getCriteriaWithoutMatch());
            }

            // Delegate the provisioning of the agent tokens to the driver according to the calculated forecast
            AgentProvisioningRequest request = new AgentProvisioningRequest();
            request.executionId = context.getExecutionId();
            request.agentPoolRequirementSpecs = requiredAgentPools;
            request.restrictions = context.get(AgentProvisioningRestrictions.class); // may be null if no restrictions present

            String provisioningRequestId = agentProvisioningDriver.initializeTokenProvisioningRequest(request);
            context.put(PROVISIONING_REQUEST_ID, provisioningRequestId);

            try {
                agentProvisioningDriver.executeTokenProvisioningRequest(provisioningRequestId);
            } catch (ProvisioningException e) {
                throw e;
            } catch (Exception e) {
                throw new ProvisioningException("Error while provisioning agents", e);
            } finally {
                // persist the last provisioning status that contains error details in case of error
                AgentProvisioningStatus agentProvisioningStatus = agentProvisioningDriver.getTokenProvisioningStatus(provisioningRequestId);
                if (agentProvisioningStatus != null) {
                    agentProvisioningStatus.executionId = context.getExecutionId();
                    agentProvisioningStatusAccessor.save(agentProvisioningStatus);
                    // now add it as reference to the execution
                    context.getExecutionManager().updateExecution(execution -> execution.addCustomField(AGENT_PROVISIONING_STATUS_ID_CUSTOM_FIELD, agentProvisioningStatus.getId().toHexString()));
                } else {
                    // Not thrown: an exception from this finally block would hide the provisioning error being propagated
                    logger.warn("No provisioning status returned by the driver for request {} of execution {}", provisioningRequestId, context.getExecutionId());
                }
            }
            logger.info("Successfully provisioned agents for execution " + context.getExecutionId());
        } else {
            logger.info("Agent provisioning is disabled for this plan");
        }
    }

    private static AgentProvisioningConfiguration getPlanAgentPoolConfiguratorOrDefault(ExecutionContext context) {
        return Objects.requireNonNullElse(context.getPlan().getAgents(), new AutomaticAgentProvisioningConfiguration(auto_detect));
    }

    @Override
    public void deprovisionRequiredResources(ExecutionContext context) {
        String provisioningRequestId = getProvisioningRequestId(context);
        if (provisioningRequestId != null) {
            logger.info("De-provisioning agents for execution " + context.getExecutionId());
            // Remove the provisioning request ID from the context before calling the driver as the corresponding request
            // will be removed from the driver when calling deprovisionTokens
            context.remove(PROVISIONING_REQUEST_ID);
            try {
                agentProvisioningDriver.deprovisionTokens(provisioningRequestId);
            } catch (Exception e) {
                throw new DeprovisioningException("Error while de-provisioning agents", e);
            }
            logger.info("Successfully de-provisioned agents for execution " + context.getExecutionId());
        }
    }

    public static String getProvisioningRequestId(ExecutionContext context) {
        return (String) context.get(PROVISIONING_REQUEST_ID);
    }
}
