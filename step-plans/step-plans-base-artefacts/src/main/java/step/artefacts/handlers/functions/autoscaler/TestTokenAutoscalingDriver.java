package step.artefacts.handlers.functions.autoscaler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class TestTokenAutoscalingDriver implements TokenAutoscalingDriver{

    public static final String CONFIGURATION_KEY = "grid.tokens.pools";

    @Override
    public TokenAutoscalingConfiguration getAutoscalerConfiguration() {
        String property = "{\"pools\":{\"pool1\": {\"$agenttype\": \"default\"}}}"; // context.getConfiguration().getProperty(CONFIGURATION_KEY, "{\"pools\":{\"pool1\": {\"$agenttype\": \"default\"}}}");

        ObjectMapper objectMapper = new ObjectMapper();
        TokenAutoscalingConfiguration autoscalerConfiguration;
        try {
            autoscalerConfiguration = objectMapper.readValue(property, TokenAutoscalingConfiguration.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error while parsing autoscaler configuration from property: " + CONFIGURATION_KEY, e);
        }
        TokenAutoscalingConfiguration autoscalerConfiguration1 = new TokenAutoscalingConfiguration();
        autoscalerConfiguration1.availableTokenPools = Map.of("defaultPool", Map.of());
        return autoscalerConfiguration1;
    }

    @Override
    public void provisionTokens(TokenProvisioningRequest request) {
        // This default implementation doesn't perform anything
    }
}
