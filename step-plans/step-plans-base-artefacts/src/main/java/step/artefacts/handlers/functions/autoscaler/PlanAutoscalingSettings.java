package step.artefacts.handlers.functions.autoscaler;

import java.util.Set;

public class PlanAutoscalingSettings {

    public static final String AUTOSCALING_SETTINGS = "autoscalingSettings";

    public boolean enableAutoscaling = true;
    public boolean enableAutomaticTokenNumberCalculation = true;
    public Set<AgentPoolRequirementSpec> requiredAgentPools;
}
