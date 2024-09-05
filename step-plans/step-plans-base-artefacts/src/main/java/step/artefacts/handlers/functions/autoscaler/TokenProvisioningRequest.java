package step.artefacts.handlers.functions.autoscaler;

import step.core.plans.agents.configuration.AgentPoolRequirementSpec;

import java.util.List;

public class TokenProvisioningRequest {

    public String executionId;
    public List<AgentPoolRequirementSpec> agentPoolRequirementSpecs;
}
