package step.core.agents.provisioning.driver;

import step.core.agents.provisioning.AgentPoolSpec;

import java.util.Set;

public class AgentProvisioningDriverConfiguration {

    public Set<AgentPoolSpec> availableAgentPools;

    public AgentProvisioningDriverConfiguration() {}

    public AgentProvisioningDriverConfiguration(Set<AgentPoolSpec> availableAgentPools) {
        this.availableAgentPools = availableAgentPools;
    }
}
