package step.core.plans.agents.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.List;

public class AgentProvisioningConfigurationDeserializer extends JsonDeserializer {
    @Override
    public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonToken currentToken = jsonParser.currentToken();
        if (currentToken == JsonToken.START_ARRAY) {
            ManualAgentProvisioningConfiguration manualAgentProvisioningConfiguration = new ManualAgentProvisioningConfiguration();
            manualAgentProvisioningConfiguration.configuredAgentPools = jsonParser.readValueAs(new TypeReference<List<AgentPoolProvisioningConfiguration>>() {});
            return manualAgentProvisioningConfiguration;
        } else {
            return new AutomaticAgentProvisioningConfiguration(AutomaticAgentProvisioningConfiguration.PlanAgentsPoolAutoMode.valueOf(jsonParser.readValueAs(String.class)));
        }
    }
}
