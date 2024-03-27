package step.artefacts.handlers.functions.autoscaler;

import java.util.Map;

public class DefaultTokenAutoscalingDriver implements TokenAutoscalingDriver {

    @Override
    public TokenAutoscalingConfiguration getAutoscalerConfiguration() {
        TokenAutoscalingConfiguration autoscalerConfiguration = new TokenAutoscalingConfiguration();
        autoscalerConfiguration.availableTokenPools = Map.of();
        return autoscalerConfiguration;
    }

    @Override
    public void provisionTokens(TokenProvisioningRequest request) {
        // This default implementation doesn't perform anything
    }
}
