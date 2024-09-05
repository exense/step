package step.plans.parser.yaml;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import step.core.plans.agents.configuration.*;
import step.core.yaml.deserializers.StepYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializerAddOn;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import static step.core.plans.agents.configuration.AgentPoolConfiguration.*;

@StepYamlDeserializerAddOn(targetClasses = {PlanAgentsConfigurationYaml.class})
public class YamlPlanAgentConfigurationDeserializer extends StepYamlDeserializer<PlanAgentsConfigurationYaml>  {

    //Required to be added to the deserializers
    public YamlPlanAgentConfigurationDeserializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }

    @Override
    public PlanAgentsConfigurationYaml deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        //[{"agentPoolTemplateName":"myPool","numberOfAgents":1},{"agentPoolTemplateName":"myPool2","numberOfAgents":2,"image":"test-image"}]
        if (node.isArray()) {
            PlanAgentPoolsConfiguration planAgentPoolsConfiguration = new PlanAgentPoolsConfiguration();
            planAgentPoolsConfiguration.configuredAgentPools = new ArrayList<>();
            for (JsonNode jsonNode : node) {
                JsonNode templateNode = jsonNode.get(TEMPLATE_PROPERTY_NAME);
                if (templateNode == null) {
                    throw new RemoteException(TEMPLATE_PROPERTY_NAME + " is mandatory and cannot be null");
                }
                JsonNode numberOfAgentsNode = jsonNode.get(NUMBER_AGENT_PROPERTY_NAME);
                if (numberOfAgentsNode == null) {
                    throw new RemoteException(NUMBER_AGENT_PROPERTY_NAME + " is mandatory and cannot be null");
                }
                JsonNode imageNode = jsonNode.get(IMAGE_PROPERTY_NAME);
                String image = (imageNode != null) ? imageNode.asText() : null;
                AgentPoolConfiguration agentPoolConfiguration = new AgentPoolConfiguration(templateNode.asText(),
                        image,
                        numberOfAgentsNode.asInt());
                planAgentPoolsConfiguration.configuredAgentPools.add(agentPoolConfiguration);
            }
            return planAgentPoolsConfiguration;
        } else {
            return  new PlanAgentsPoolsAutoConfiguration(PlanAgentsPoolsAutoConfiguration.PlanAgentsPoolAutoMode.valueOf(node.asText()));
        }
    }
}
