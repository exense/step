package step.core.plans.agents.configuration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonSubTypes({
        @JsonSubTypes.Type(value = PlanAgentsPoolsAutoConfiguration.class),
        @JsonSubTypes.Type(value = PlanAgentPoolsConfiguration.class)
})
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, property="_class")
public interface PlanAgentsConfiguration extends PlanAgentsConfigurationInterface {

    PlanAgentsConfigurationYaml asYamlConfiguration();

}
