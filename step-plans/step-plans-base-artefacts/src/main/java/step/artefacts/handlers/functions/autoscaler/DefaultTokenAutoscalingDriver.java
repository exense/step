package step.artefacts.handlers.functions.autoscaler;

import ch.exense.commons.app.Configuration;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static step.artefacts.handlers.functions.autoscaler.AgentPoolProvisioningParameters.TOKEN_ATTRIBUTE_DOCKER_SUPPORT;

public class DefaultTokenAutoscalingDriver implements TokenAutoscalingDriver {

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, TokenProvisioningRequest> provisioningRequest = new ConcurrentHashMap<>();
    private final Map<String, TokenProvisioningStatus> provisioningStatus = new ConcurrentHashMap<>();

    public DefaultTokenAutoscalingDriver(Configuration configuration) {

    }

    @Override
    public TokenAutoscalingConfiguration getConfiguration() {
        TokenAutoscalingConfiguration autoscalerConfiguration = new TokenAutoscalingConfiguration();
        autoscalerConfiguration.availableAgentPools = Set.of(new AgentPoolSpec("DefaultPool", Map.of("$agenttype", "default", TOKEN_ATTRIBUTE_DOCKER_SUPPORT, "true"), 1));
        return autoscalerConfiguration;
    }

    @Override
    public String initializeTokenProvisioningRequest(TokenProvisioningRequest request) {
        String provisioningId = UUID.randomUUID().toString();
        TokenProvisioningStatus status = new TokenProvisioningStatus();
        status.statusDescription = "Provisioning tokens... (MOCK)";
        provisioningRequest.put(provisioningId, request);
        provisioningStatus.put(provisioningId, status);
        return provisioningId;
    }

    @Override
    public TokenProvisioningStatus executeTokenProvisioningRequest(String provisioningRequestId) {
        TokenProvisioningRequest tokenProvisioningRequest = provisioningRequest.get(provisioningRequestId);
        TokenProvisioningStatus tokenProvisioningStatus = provisioningStatus.get(provisioningRequestId);

        Future<?> future = executorService.submit(() -> {
            for (int i = 0; i <= tokenProvisioningRequest.agentPoolRequirementSpecs.size(); i++) {
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
            tokenProvisioningStatus.completed = true;
        }

        return tokenProvisioningStatus;
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
    public TokenProvisioningStatus getTokenProvisioningStatus(String provisioningRequestId) {
        return provisioningStatus.get(provisioningRequestId);
    }
}
