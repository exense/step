package step.core.plans.agents.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public interface PlanAgentsConfigurationInterface {
    @JsonIgnore
    List<AgentPoolRequirementSpec> getAgentPoolRequirementSpecs();

    boolean enableAutomaticTokenNumberCalculation();

    public abstract boolean enableAutoScaling();
}
