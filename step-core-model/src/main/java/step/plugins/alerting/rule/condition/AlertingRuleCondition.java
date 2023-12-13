package step.plugins.alerting.rule.condition;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = AlertingRuleCondition.JSON_CLASS_PROPERTY)
public abstract class AlertingRuleCondition {
    public static final String JSON_CLASS_PROPERTY = "class";

    public String description;

    public boolean active = true;
    public boolean negate = false;
}
