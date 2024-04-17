package step.artefacts.handlers.functions.autoscaler;

import java.util.Map;

public class TokenProvisioningRequest {

    public String executionId;
    public Map<String, Integer> requiredNumberOfTokensPerPool;
}
