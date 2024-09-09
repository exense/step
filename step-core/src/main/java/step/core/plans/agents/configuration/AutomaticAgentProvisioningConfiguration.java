package step.core.plans.agents.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import step.core.agents.provisioning.AgentPoolRequirementSpec;

import java.util.List;

public class AutomaticAgentProvisioningConfiguration implements AgentProvisioningConfiguration {

    public final PlanAgentsPoolAutoMode mode;

    public AutomaticAgentProvisioningConfiguration(@JsonProperty("mode") PlanAgentsPoolAutoMode mode) {
        this.mode = mode;
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
    public List<AgentPoolRequirementSpec> getAgentPoolRequirementSpecs() {
        throw new IllegalStateException("getAgentPoolRequirementSpecs shouldn't be called when enableAutomaticTokenNumberCalculation is set to true");
    }

    public enum PlanAgentsPoolAutoMode {
        auto_detect;
    }
}
