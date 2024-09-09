package step.core.plans.agents.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class AgentProvisioningConfigurationSerializer extends JsonSerializer<AgentProvisioningConfiguration> {
    @Override
    public void serialize(AgentProvisioningConfiguration agentProvisioningConfiguration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if(agentProvisioningConfiguration instanceof ManualAgentProvisioningConfiguration) {
            ManualAgentProvisioningConfiguration manualAgentProvisioningConfiguration = (ManualAgentProvisioningConfiguration) agentProvisioningConfiguration;
            jsonGenerator.writePOJO(manualAgentProvisioningConfiguration.configuredAgentPools);
        } else if (agentProvisioningConfiguration instanceof AutomaticAgentProvisioningConfiguration) {
            String mode = ((AutomaticAgentProvisioningConfiguration) agentProvisioningConfiguration).mode.toString();
            jsonGenerator.writeString(mode);
        } else {
            jsonGenerator.writePOJO(agentProvisioningConfiguration);
        }
    }
}
