package step.artefacts.handlers.functions.autoscaler;

import java.util.Map;
import java.util.Set;

public class TokenProvisioningRequest {

    public String executionId;
    public Map<String, Integer> requiredNumberOfTokensPerPool;

    public Set<CustomAgentPoolSpec> customAgentPools;
}
