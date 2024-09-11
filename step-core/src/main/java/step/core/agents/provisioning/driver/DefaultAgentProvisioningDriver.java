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

import ch.exense.commons.app.Configuration;
import step.core.agents.provisioning.AgentPoolSpec;
import step.grid.Grid;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static step.core.agents.provisioning.AgentPoolProvisioningParameters.TOKEN_ATTRIBUTE_DOCKER_SUPPORT;

public class DefaultAgentProvisioningDriver implements AgentProvisioningDriver {

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, AgentProvisioningRequest> provisioningRequest = new ConcurrentHashMap<>();
    private final Map<String, AgentProvisioningStatus> provisioningStatus = new ConcurrentHashMap<>();

    public DefaultAgentProvisioningDriver(Configuration configuration, Grid grid) {

    }

    @Override
    public AgentProvisioningDriverConfiguration getConfiguration() {
        AgentProvisioningDriverConfiguration configuration = new AgentProvisioningDriverConfiguration();
        configuration.availableAgentPools = Set.of(new AgentPoolSpec("DefaultPool", Map.of("$agenttype", "default", TOKEN_ATTRIBUTE_DOCKER_SUPPORT, "true"), 1));
        return configuration;
    }

    @Override
    public String initializeTokenProvisioningRequest(AgentProvisioningRequest request) {
        String provisioningId = UUID.randomUUID().toString();
        AgentProvisioningStatus status = new AgentProvisioningStatus();
        status.statusDescription = "Provisioning tokens... (MOCK)";
        provisioningRequest.put(provisioningId, request);
        provisioningStatus.put(provisioningId, status);
        return provisioningId;
    }

    @Override
    public AgentProvisioningStatus executeTokenProvisioningRequest(String provisioningRequestId) {
        AgentProvisioningRequest agentProvisioningRequest = provisioningRequest.get(provisioningRequestId);
        AgentProvisioningStatus agentProvisioningStatus = provisioningStatus.get(provisioningRequestId);

        Future<?> future = executorService.submit(() -> {
            for (int i = 0; i <= agentProvisioningRequest.agentPoolRequirementSpecs.size(); i++) {
                sleep();
            }
        });
        try {
            future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            agentProvisioningStatus.completed = true;
        }

        return agentProvisioningStatus;
    }

    private static void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deprovisionTokens(String provisioningRequestId) {
        sleep();
        provisioningRequest.remove(provisioningRequestId);
    }

    @Override
    public AgentProvisioningStatus getTokenProvisioningStatus(String provisioningRequestId) {
        return provisioningStatus.get(provisioningRequestId);
    }
}
