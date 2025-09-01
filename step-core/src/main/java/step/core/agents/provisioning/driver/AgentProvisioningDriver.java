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

import step.core.agents.provisioning.AgentPoolSpec;

import java.util.Set;

public interface AgentProvisioningDriver {

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
     * @param provisioningRequestId the unique id of the request
     * @return the status of the request or null if the request doesn't exist or completed
     */
    AgentProvisioningStatus getTokenProvisioningStatus(String provisioningRequestId);

    /**
     * Performs the deprovisioning of the token provisioned previously for the provided request id
     * @param provisioningRequestId the unique id of the provisioning request
     */
    void deprovisionTokens(String provisioningRequestId) throws Exception;

    /**
     * Register or keep alive a remote agent pool template. Registered templates are evicted after a configurable TTL
     * This method has to be called periodically to keep them alive
     * @param agentPoolSpecs the specification of the remote agent pool to be registered
     */
    void registerRemoteAgentPoolSpecs(Set<AgentPoolSpec> agentPoolSpecs);
}
