package step.plans.parser.yaml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Test;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.plans.Plan;
import step.core.plans.agents.configuration.AgentPoolProvisioningConfiguration;
import step.core.plans.agents.configuration.AutomaticAgentProvisioningConfiguration;
import step.core.plans.agents.configuration.ManualAgentProvisioningConfiguration;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class YamlPlanSerializationTest {

    @Test
    public void asYamlConfiguration() throws JsonProcessingException {
        ObjectMapper objectMapper = DefaultJacksonMapperProvider.getObjectMapper(new YAMLFactory());

        YamlPlan yamlPlan = objectMapper.readValue(
                "agents:\n" +
                "- replicas: 0\n" +
                "  pool: \"hhh\"\n" +
                "  image: null\n", YamlPlan.class);

        objectMapper.writeValueAsString(yamlPlan);

        yamlPlan = objectMapper.readValue(
                "agents: auto_detect\n", YamlPlan.class);

        objectMapper.writeValueAsString(yamlPlan);

        yamlPlan = objectMapper.readValue("name: myName", YamlPlan.class);
        assertNull(yamlPlan.getAgents());

        ObjectMapper jsonObjectMapper = DefaultJacksonMapperProvider.getObjectMapper();

        Plan plan = new Plan();
        plan.setAgents(new AutomaticAgentProvisioningConfiguration(AutomaticAgentProvisioningConfiguration.PlanAgentsPoolAutoMode.auto_detect));

        String planStr = jsonObjectMapper.writeValueAsString(plan);
        plan = jsonObjectMapper.readValue(planStr, Plan.class);

        assertEquals(AutomaticAgentProvisioningConfiguration.PlanAgentsPoolAutoMode.auto_detect, ((AutomaticAgentProvisioningConfiguration) plan.getAgents()).mode);

        ManualAgentProvisioningConfiguration agents = new ManualAgentProvisioningConfiguration();
        agents.configuredAgentPools = List.of(new AgentPoolProvisioningConfiguration());
        plan.setAgents(agents);

        planStr = jsonObjectMapper.writeValueAsString(plan);
        plan = jsonObjectMapper.readValue(planStr, Plan.class);

        assertEquals(agents.configuredAgentPools, ((ManualAgentProvisioningConfiguration) plan.getAgents()).configuredAgentPools);
    }
}