package step.plans.parser.yaml;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import step.core.plans.agents.configuration.*;
import step.core.yaml.serializers.StepYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializerAddOn;

import java.io.IOException;

import static step.core.plans.agents.configuration.AgentPoolConfiguration.*;

@StepYamlSerializerAddOn(targetClasses = {PlanAgentsConfigurationYaml.class})
public class YamlPlanAgentConfigurationSerializer extends StepYamlSerializer<PlanAgentsConfigurationYaml> {

    //Required to be added to the deserializers
    public YamlPlanAgentConfigurationSerializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }


    @Override
    public void serialize(PlanAgentsConfigurationYaml planAgentsConfigurator, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (planAgentsConfigurator instanceof PlanAgentsPoolsAutoConfiguration) {
            jsonGenerator.writeString(((PlanAgentsPoolsAutoConfiguration) planAgentsConfigurator).mode.name());
        } else if (planAgentsConfigurator instanceof PlanAgentPoolsConfiguration) {
            PlanAgentPoolsConfiguration planAgentPoolsConfiguration = (PlanAgentPoolsConfiguration) planAgentsConfigurator;
            jsonGenerator.writeStartArray();
            for (AgentPoolConfiguration p : planAgentPoolsConfiguration.configuredAgentPools) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField(TEMPLATE_PROPERTY_NAME, p.templateName);
                if (p.image != null) {
                    jsonGenerator.writeStringField(IMAGE_PROPERTY_NAME, p.image);
                }
                jsonGenerator.writeNumberField(NUMBER_AGENT_PROPERTY_NAME, p.number);
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
        } else {
            throw new RuntimeException("YamlPlanSerializer only support PlanAgentsPoolsAutoConfiguration and PlanAgentPoolsConfiguration");
        }
    }
}
