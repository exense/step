package step.artefacts.handlers.functions.autoscaler;

import java.util.Map;
import java.util.Set;

/**
 * This class represents a configured agent pool that can be referenced in {@link AgentPoolRequirementSpec}
 */
public class AgentPoolSpec {

    public String name;
    public String displayName;
    public Map<String, String> attributes;
    public int numberOfTokens;
    public Set<AgentPoolProvisioningParameter> supportedProvisioningParameters;

    public AgentPoolSpec() {
    }

    public AgentPoolSpec(String name, Map<String, String> attributes, int numberOfTokens) {
        this(name, attributes, numberOfTokens, Set.of());
    }

    public AgentPoolSpec(String name, Map<String, String> attributes, int numberOfTokens, Set<AgentPoolProvisioningParameter> supportedProvisioningParameters) {
        this(name, name, attributes, numberOfTokens, supportedProvisioningParameters);
    }

    public AgentPoolSpec(String name, String displayName, Map<String, String> attributes, int numberOfTokens, Set<AgentPoolProvisioningParameter> supportedProvisioningParameters) {
        this.name = name;
        this.displayName = displayName;
        this.attributes = attributes;
        this.numberOfTokens = numberOfTokens;
        this.supportedProvisioningParameters = supportedProvisioningParameters;
    }

    @Override
    public String toString() {
        return "AgentPoolSpec{" +
                "name='" + name + '\'' +
                ", attributes=" + attributes +
                ", numberOfTokens=" + numberOfTokens +
                '}';
    }
}
