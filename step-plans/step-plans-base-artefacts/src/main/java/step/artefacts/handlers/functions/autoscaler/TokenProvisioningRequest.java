package step.artefacts.handlers.functions.autoscaler;

import step.core.agents.provisioning.AgentPoolRequirementSpec;

import java.util.List;

public class TokenProvisioningRequest {

    public String executionId;
    public List<AgentPoolRequirementSpec> agentPoolRequirementSpecs;
}
