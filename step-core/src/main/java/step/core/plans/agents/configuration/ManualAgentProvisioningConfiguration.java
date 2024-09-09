package step.core.plans.agents.configuration;

import step.core.agents.provisioning.AgentPoolProvisioningParameters;
import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.core.yaml.YamlModel;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.jsonschema.JsonSchema;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//Not scanned for now
@YamlModel(name = ManualAgentProvisioningConfiguration.AGENT_CONFIGURATION_YAML_NAME)
public class ManualAgentProvisioningConfiguration implements AgentProvisioningConfiguration {

    public static final String AGENT_CONFIGURATION_YAML_NAME = "agents";
    public static final String AGENT_POOL_CONFIGURATION_ARRAY_DEF = "agentPoolConfigurationArrayDef";

    @JsonSchema(ref = YamlJsonSchemaHelper.DEFS_PREFIX + AGENT_POOL_CONFIGURATION_ARRAY_DEF)
    public List<AgentPoolProvisioningConfiguration> configuredAgentPools;

    public List<AgentPoolRequirementSpec> getAgentPoolRequirementSpecs() {
        if (configuredAgentPools != null) {
            return configuredAgentPools.stream().map(p -> new AgentPoolRequirementSpec(p.pool, Map.of(AgentPoolProvisioningParameters.PROVISIONING_PARAMETER_DOCKER_IMAGE, p.image), p.replicas)).collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    @Override
    public boolean enableAutomaticTokenNumberCalculation() {
        return false;
    }

    @Override
    public boolean enableAutoScaling() {
        return configuredAgentPools != null && !configuredAgentPools.isEmpty();
    }
}
