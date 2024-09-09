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
        AgentProvisioningDriverConfiguration autoscalerConfiguration = new AgentProvisioningDriverConfiguration();
        autoscalerConfiguration.availableAgentPools = Set.of(new AgentPoolSpec("DefaultPool", Map.of("$agenttype", "default", TOKEN_ATTRIBUTE_DOCKER_SUPPORT, "true"), 1));
        return autoscalerConfiguration;
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
