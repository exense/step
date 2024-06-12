package step.artefacts.handlers.functions.autoscaler;

import java.util.Map;

/**
 * This class represents a configured agent pool that can be referenced in {@link AgentPoolRequirementSpec}
 */
public class AgentPoolSpec {

    public String name;
    public Map<String, String> attributes;
    public int numberOfTokens;

    public AgentPoolSpec(String name, Map<String, String> attributes, int numberOfTokens) {
        this.name = name;
        this.attributes = attributes;
        this.numberOfTokens = numberOfTokens;
    }
}
