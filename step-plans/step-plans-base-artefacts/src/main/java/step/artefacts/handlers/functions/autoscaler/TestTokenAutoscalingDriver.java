package step.artefacts.handlers.functions.autoscaler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

public class TestTokenAutoscalingDriver implements TokenAutoscalingDriver{

    public static final String CONFIGURATION_KEY = "grid.tokens.pools";

    @Override
    public TokenAutoscalingConfiguration getConfiguration() {
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
    public void executeTokenProvisioningRequest(String provisioningRequestId) {

    }

    @Override
    public void deprovisionTokens(String provisioningRequestId) {

    }

    @Override
    public TokenProvisioningStatus getTokenProvisioningStatus(String provisioningRequestId) {
        return new TokenProvisioningStatus();
    }

    @Override
    public String initializeTokenProvisioningRequest(TokenProvisioningRequest request) {
        return UUID.randomUUID().toString();
    }
}
