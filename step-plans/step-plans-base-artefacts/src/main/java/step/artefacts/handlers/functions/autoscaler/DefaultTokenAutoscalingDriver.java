package step.artefacts.handlers.functions.autoscaler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class DefaultTokenAutoscalingDriver implements TokenAutoscalingDriver {

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, TokenProvisioningStatus> provisioningRequest = new ConcurrentHashMap<>();


    @Override
    public TokenAutoscalingConfiguration getConfiguration() {
        TokenAutoscalingConfiguration autoscalerConfiguration = new TokenAutoscalingConfiguration();
        autoscalerConfiguration.availableTokenPools = Map.of("DefaultPool", Map.of("$agenttype", "default"));
        return autoscalerConfiguration;
    }

    @Override
    public String initializeTokenProvisioningRequest(TokenProvisioningRequest request) {
        String provisioningId = UUID.randomUUID().toString();
        TokenProvisioningStatus status = new TokenProvisioningStatus();
        status.tokenCountTarget = request.requiredNumberOfTokensPerPool.values().stream().reduce(0, Integer::sum);
        status.statusDescription = "Provisioning tokens... (MOCK)";
        provisioningRequest.put(provisioningId, status);
        return provisioningId;
    }

    @Override
    public void executeTokenProvisioningRequest(String provisioningRequestId) {
        TokenProvisioningStatus tokenProvisioningStatus = provisioningRequest.get(provisioningRequestId);

        Future<?> future = executorService.submit(() -> {
            for (int i = 0; i < tokenProvisioningStatus.tokenCountTarget; i++) {
                sleep();
                tokenProvisioningStatus.tokenCountStarted = i;
            }
        });
        try {
            future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
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
        return provisioningRequest.get(provisioningRequestId);
    }
}
