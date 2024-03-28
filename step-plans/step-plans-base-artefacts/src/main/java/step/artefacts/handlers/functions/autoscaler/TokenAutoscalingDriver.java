package step.artefacts.handlers.functions.autoscaler;

public interface TokenAutoscalingDriver {

    TokenAutoscalingConfiguration getConfiguration();

    String initializeTokenProvisioningRequest(TokenProvisioningRequest request);

    void executeTokenProvisioningRequest(String provisioningRequestId);

    TokenProvisioningStatus getTokenProvisioningStatus(String provisioningRequestId);

    void deprovisionTokens(String provisioningRequestId);
}
