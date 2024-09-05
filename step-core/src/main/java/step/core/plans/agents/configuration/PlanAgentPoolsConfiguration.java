package step.core.plans.agents.configuration;

import step.core.yaml.YamlModel;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.jsonschema.JsonSchema;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//Not scanned for now
@YamlModel(name = PlanAgentPoolsConfiguration.AGENT_CONFIGURATION_YAML_NAME)
public class PlanAgentPoolsConfiguration implements PlanAgentsConfiguration, PlanAgentsConfigurationYaml {

    public static final String AGENT_CONFIGURATION_YAML_NAME = "agents";
    public static final String AGENT_POOL_CONFIGURATION_ARRAY_DEF = "agentPoolConfigurationArrayDef";
    public static final String PROVISIONING_PARAMETER_DOCKER_IMAGE = "dockerImage";

    @JsonSchema(ref = YamlJsonSchemaHelper.DEFS_PREFIX + AGENT_POOL_CONFIGURATION_ARRAY_DEF)
    public List<AgentPoolConfiguration> configuredAgentPools;

    @Override
    public List<AgentPoolRequirementSpec> getAgentPoolRequirementSpecs() {
        if (configuredAgentPools != null) {
            return configuredAgentPools.stream().map(p -> new AgentPoolRequirementSpec(p.templateName, Map.of(PROVISIONING_PARAMETER_DOCKER_IMAGE, p.image), p.number)).collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public boolean enableAutomaticTokenNumberCalculation() {
        return configuredAgentPools == null;
    }

    @Override
    public boolean enableAutoScaling() {
        //if requiredAgentPools is null, we're in full auto; if pools are defined in manual scaling
        // Otherwise (empty list of pool) we disable auto scaling
        return configuredAgentPools == null || !configuredAgentPools.isEmpty();
    }

    @Override
    public PlanAgentsConfigurationYaml asYamlConfiguration() {
        return this;
    }

    @Override
    public PlanAgentsConfiguration asNativeConfiguration() {
        return this;
    }
}
