package step.artefacts.handlers.functions.autoscaler;

import step.core.agents.provisioning.AgentPoolSpec;

import java.util.Set;

public class TokenAutoscalingConfiguration {

    public Set<AgentPoolSpec> availableAgentPools;

    public TokenAutoscalingConfiguration() {}

    public TokenAutoscalingConfiguration(Set<AgentPoolSpec> availableAgentPools) {
        this.availableAgentPools = availableAgentPools;
    }
}
