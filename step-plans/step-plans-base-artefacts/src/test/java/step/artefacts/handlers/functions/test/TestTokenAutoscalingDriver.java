package step.artefacts.handlers.functions.test;

import step.artefacts.handlers.functions.autoscaler.TokenAutoscalingConfiguration;
import step.artefacts.handlers.functions.autoscaler.TokenAutoscalingDriver;
import step.artefacts.handlers.functions.autoscaler.TokenProvisioningRequest;
import step.artefacts.handlers.functions.autoscaler.TokenProvisioningStatus;

import java.util.UUID;

public class TestTokenAutoscalingDriver implements TokenAutoscalingDriver {

    private final TokenAutoscalingConfiguration configuration;
    private TokenProvisioningRequest request;

    public TestTokenAutoscalingDriver(TokenAutoscalingConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public TokenAutoscalingConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void executeTokenProvisioningRequest(String provisioningRequestId) {

    }

    @Override
    public void deprovisionTokens(String provisioningRequestId) {

    }

    @Override
    public TokenProvisioningStatus getTokenProvisioningStatus(String provisioningRequestId) {
        return new TokenProvisioningStatus();
    }

    public TokenProvisioningRequest getRequest() {
        return request;
    }

    @Override
    public String initializeTokenProvisioningRequest(TokenProvisioningRequest request) {
        this.request = request;
        return UUID.randomUUID().toString();
    }
}
