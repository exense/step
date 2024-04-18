package step.artefacts.handlers.functions.autoscaler;

import io.kubernetes.client.openapi.models.V1ConfigMapList;

import java.util.Map;

public class TokenAutoscalingConfiguration {

    public Map<String, Map<String, String>> availableTokenPools;

    public V1ConfigMapList availableConfigMapList;

    public TokenAutoscalingConfiguration() {}

    public TokenAutoscalingConfiguration(Map<String, Map<String, String>> availableTokenPools) {
        this.availableTokenPools = availableTokenPools;
    }
}
