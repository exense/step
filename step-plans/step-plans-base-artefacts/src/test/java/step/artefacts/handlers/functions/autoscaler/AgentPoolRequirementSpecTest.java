package step.artefacts.handlers.functions.autoscaler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static step.artefacts.handlers.functions.autoscaler.AgentPoolProvisioningParameters.DOCKER_IMAGE;

public class AgentPoolRequirementSpecTest {

    @Test
    public void testSerialization() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String test = objectMapper.writeValueAsString(new AgentPoolRequirementSpec("test", 1));
        AgentPoolRequirementSpec value = objectMapper.readValue(test, AgentPoolRequirementSpec.class);
        assertEquals(AgentPoolRequirementSpec.class, value.getClass());

        String dockerImageParameterStr = objectMapper.writeValueAsString(DOCKER_IMAGE);
        AgentPoolProvisioningParameter dockerImageParameter = objectMapper.readValue(dockerImageParameterStr, AgentPoolProvisioningParameter.class);

        AgentPoolSpec poolSpec = new AgentPoolSpec("test", Map.of(), 2, Set.of(dockerImageParameter));
        String poolSpecStr = objectMapper.writeValueAsString(poolSpec);
        objectMapper.readValue(poolSpecStr, AgentPoolSpec.class);
    }

}