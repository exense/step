package step.artefacts.handlers.functions.autoscaler;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Defines the requirements in terms of Agents (number of agents, etc) for a Plan
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({ @JsonSubTypes.Type(TemplateStsAgentPoolRequirementSpec.class) })
public abstract class AgentPoolRequirementSpec {
    public int numberOfAgents;
}
