package step.artefacts.handlers.functions.autoscaler;

public interface TokenAutoscalingDriver {

    TokenAutoscalingConfiguration getAutoscalerConfiguration();

    void provisionTokens(TokenProvisioningRequest request);
}
