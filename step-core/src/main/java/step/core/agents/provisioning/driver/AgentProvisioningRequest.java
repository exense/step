package step.core.agents.provisioning.driver;

import step.core.agents.provisioning.AgentPoolRequirementSpec;

import java.util.List;

public class AgentProvisioningRequest {

    public String executionId;
    public List<AgentPoolRequirementSpec> agentPoolRequirementSpecs;
}
