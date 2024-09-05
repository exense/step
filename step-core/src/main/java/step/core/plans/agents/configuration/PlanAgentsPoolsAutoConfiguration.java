package step.core.plans.agents.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PlanAgentsPoolsAutoConfiguration implements PlanAgentsConfiguration, PlanAgentsConfigurationYaml  {

    public final PlanAgentsPoolAutoMode mode;

    public PlanAgentsPoolsAutoConfiguration(@JsonProperty("mode") PlanAgentsPoolAutoMode mode) {
        this.mode = mode;
    }

    @Override
    public List<AgentPoolRequirementSpec> getAgentPoolRequirementSpecs() {
        return List.of();
    }

    @Override
    public boolean enableAutomaticTokenNumberCalculation() {
        return true;
    }

    @Override
    public boolean enableAutoScaling() {
        return true;
    }

    @Override
    public PlanAgentsConfigurationYaml asYamlConfiguration() {
        return this;
    }

    @Override
    public PlanAgentsConfiguration asNativeConfiguration() {
        return this;
    }

    public static enum PlanAgentsPoolAutoMode {
        auto_detect;
    }
}
