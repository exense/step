package step.core.plans.agents.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import step.core.agents.provisioning.AgentPoolRequirementSpec;

import java.util.List;

@JsonSubTypes({
        @JsonSubTypes.Type(value = AutomaticAgentProvisioningConfiguration.class),
        @JsonSubTypes.Type(value = ManualAgentProvisioningConfiguration.class)
})
public interface AgentProvisioningConfiguration {

    boolean enableAutomaticTokenNumberCalculation();

    boolean enableAutoScaling();

    @JsonIgnore
    List<AgentPoolRequirementSpec> getAgentPoolRequirementSpecs();

}
