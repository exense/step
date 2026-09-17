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

package step.core.agents.provisioning.driver;

import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.core.agents.provisioning.AgentPoolSpec;
import step.core.agents.provisioning.TokenSelectionCriteriaFilter;
import step.core.plans.agents.configuration.AgentProvisioningConfiguration;
import step.grid.tokenpool.Interest;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public interface AgentProvisioningDriver extends Closeable {

    /**
     * @return the agent provisioning configuration
     */
    AgentProvisioningDriverConfiguration getConfiguration();

    /**
     * Token provisioning requests are performed in 2 steps: initialize and execute.
     * The provisioning itself that can take a few minutes should be performed in the execute step.
     * This method corresponds to the initialize step, which is intended to prepare the execution and return quickly.
     *
     * @param request the parameters of the request
     * @return a unique id that identifies the request
     */
    String initializeTokenProvisioningRequest(AgentProvisioningRequest request);

    /**
     * Performs the provisioning request identified by the provided id and
     * previously initialized by initializeTokenProvisioningRequest
     *
     * @param provisioningRequestId the unique id of the request
     * @return the last status of the provisioning request
     */
    AgentProvisioningStatus executeTokenProvisioningRequest(String provisioningRequestId) throws Exception;

    /**
     * Returns the status of the provisioning request identified by the provided id
     *
     * @param provisioningRequestId the unique id of the request
     * @return the status of the request or null if the request doesn't exist or completed
     */
    AgentProvisioningStatus getTokenProvisioningStatus(String provisioningRequestId);

    /**
     * Performs the deprovisioning of the token provisioned previously for the provided request id
     *
     * @param provisioningRequestId the unique id of the provisioning request
     */
    void deprovisionTokens(String provisioningRequestId) throws Exception;

    /**
     * Register or keep alive a remote agent pool template. Registered templates are evicted after a configurable TTL
     * This method has to be called periodically to keep them alive
     *
     * @param agentPoolSpecs the specification of the remote agent pool to be registered
     */
    void registerRemoteAgentPoolSpecs(Set<AgentPoolSpec> agentPoolSpecs);

    /**
     * Returns the agent pools to provision for a plan whose agent pools are configured manually. The default returns
     * the configured list but custom implementation may override this behavior. Example local provisioning
     * {@code LocalProcessAgentProvisioningDriver} will resolve to a list of agents actually required for the execution
     *
     * @param agentProvisioningConfiguration the agent pool configuration of the plan, empty when the plan disables
     *                                       the provisioning
     * @param forecasted                     the agent pool requirements calculated by the token forecasting
     * @param criteriaWithoutMatch           the token selection criteria for which the token forecasting found no agent pool
     * @return the agent pool requirements to be provisioned. By default, the configured ones, shall never return null.
     */
    default List<AgentPoolRequirementSpec> resolveConfiguredAgentPools(AgentProvisioningConfiguration agentProvisioningConfiguration,
                                                                       List<AgentPoolRequirementSpec> forecasted,
                                                                       Set<Map<String, Interest>> criteriaWithoutMatch) {
        Objects.requireNonNull(agentProvisioningConfiguration, "agentProvisioningConfiguration must not be null");
        return (agentProvisioningConfiguration.enableAgentProvisioning()) ? agentProvisioningConfiguration.getAgentPoolRequirementSpecs() : List.of();
    }

    /**
     * Some driver always provision agents even when the plan disable it in its configuration.
     * Simple because the plan's configuration is mean for a deployed plan on a Step instance and
     * does not necessarily apply in other context.
     *
     * @return whether the driver always provision agents, false by default
     */
    default boolean alwaysProvisionAgents() {
        return false;
    }

    /**
     * @param criteriaWithoutMatch the token selection criteria for which the token forecasting found no agent pool
     * @return the message of the error raised when the agent pools are calculated automatically and some criteria
     * have no matching agent pool
     */
    default String getUnmatchedCriteriaMessage(Set<Map<String, Interest>> criteriaWithoutMatch) {
        return "No matching agent pool found for selection criteria: " + criteriaWithoutMatch;
    }

    /**
     * @return a new filter applied to the token selection criteria of the keywords of an execution provisioned by this
     * driver, or null if the criteria are used as defined. Called once per execution.
     */
    default TokenSelectionCriteriaFilter createTokenSelectionCriteriaFilter() {
        return null;
    }

    default void close() {
        // Default implementation does nothing
    }
}
