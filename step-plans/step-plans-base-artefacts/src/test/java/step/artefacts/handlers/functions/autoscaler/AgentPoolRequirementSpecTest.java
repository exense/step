package step.artefacts.handlers.functions.autoscaler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.*;

public class AgentPoolRequirementSpecTest {

    @Test
    public void testSerialization() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String test = objectMapper.writeValueAsString(new TemplateStsAgentPoolRequirementSpec("test", 1));
        AgentPoolRequirementSpec value = objectMapper.readValue(test, AgentPoolRequirementSpec.class);
        assertEquals(TemplateStsAgentPoolRequirementSpec.class, value.getClass());
    }

}