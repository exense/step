package step.artefacts.handlers.functions.autoscaler;

import java.util.Map;

public class TokenAutoscalingConfiguration {

    public Map<String, Map<String, String>> availableTokenPools;

    public TokenAutoscalingConfiguration() {}

    public TokenAutoscalingConfiguration(Map<String, Map<String, String>> availableTokenPools) {
        this.availableTokenPools = availableTokenPools;
    }
}
