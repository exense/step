package step.artefacts.handlers.functions.autoscaler;

import java.util.Map;
import java.util.Objects;

/**
 * Defines the requirements in terms of Agents for a Plan
 */
public class AgentPoolRequirementSpec {
    public int numberOfAgents;
    public String agentPoolTemplateName;
    public Map<String, String> provisioningParameters;

    public AgentPoolRequirementSpec() {
    }

    public AgentPoolRequirementSpec(String agentPoolTemplateName, int numberOfAgents) {
        this(agentPoolTemplateName, Map.of(), numberOfAgents);
    }
    public AgentPoolRequirementSpec(String agentPoolTemplateName, Map<String, String> provisioningParameters, int numberOfAgents) {
        this.numberOfAgents = numberOfAgents;
        this.agentPoolTemplateName = agentPoolTemplateName;
        this.provisioningParameters = provisioningParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentPoolRequirementSpec that = (AgentPoolRequirementSpec) o;
        return numberOfAgents == that.numberOfAgents && Objects.equals(agentPoolTemplateName, that.agentPoolTemplateName) && Objects.equals(provisioningParameters, that.provisioningParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfAgents, agentPoolTemplateName, provisioningParameters);
    }
}
