package step.artefacts.handlers.functions.autoscaler;

import step.core.yaml.YamlModel;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.jsonschema.JsonSchema;

import java.util.List;

//Not scanned for now
@YamlModel(name = PlanAutoscalingSettings.AUTOSCALING_SETTINGS)
public class PlanAutoscalingSettings {

    public static final String AUTOSCALING_SETTINGS = "autoscalingSettings";
    public static final String AGENT_POOL_ARRAY_DEF = "agentPoolRequirementSpecArrayDef";

    public boolean enableAutoscaling = true;
    public boolean enableAutomaticTokenNumberCalculation = true;
    @JsonSchema(ref = YamlJsonSchemaHelper.DEFS_PREFIX + AGENT_POOL_ARRAY_DEF)
    public List<AgentPoolRequirementSpec> requiredAgentPools;
}
