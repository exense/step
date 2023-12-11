package step.plugins.alerting.rule.condition;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public abstract class AlertingRuleCondition {
    public String description;

    public boolean active = true;
    public boolean negate = false;
}
