package step.artefacts.handlers.functions.autoscaler;

import java.util.Set;

public class TokenProvisioningRequest {

    public String executionId;
    public Set<AgentPoolRequirementSpec> agentPoolRequirementSpecs;
}
