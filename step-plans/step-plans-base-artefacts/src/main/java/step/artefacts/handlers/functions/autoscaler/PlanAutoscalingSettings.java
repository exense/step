package step.artefacts.handlers.functions.autoscaler;

import java.util.Map;

public class PlanAutoscalingSettings {

    public static final String AUTOSCALING_SETTINGS = "autoscalingSettings";

    public boolean enableAutoscaling = true;
    public boolean enableAutomaticTokenNumberCalculation = true;
    public Map<String, Integer> requiredNumberOfTokens;
}
