package step.migration.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.collections.Collection;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.plans.Plan;
import step.core.plans.agents.configuration.AgentPoolConfiguration;
import step.core.plans.agents.configuration.PlanAgentPoolsConfiguration;
import step.core.plans.agents.configuration.PlanAgentsConfiguration;
import step.core.plans.agents.configuration.PlanAgentsPoolsAutoConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MigratePlanAgentsConfigurationTest {

	@Test
	public void testAutoScalingOfConfig() throws IOException {
		try (InputStream is = this.getClass().getResourceAsStream("oldPlanWithAutoscalingSettingsOf.json")) {
			ObjectMapper mapper = DefaultJacksonMapperProvider.getObjectMapper();
			Document oldPlan = mapper.readValue(is, Document.class);
			InMemoryCollectionFactory collectionFactory = new InMemoryCollectionFactory(new Properties());
			Collection<Document> plans = collectionFactory.getCollection("plans", step.core.collections.Document.class);
			Collection<Plan> actualPlans = collectionFactory.getCollection("plans", Plan.class);
			plans.save(oldPlan);
			MigratePlanAgentsConfiguration migratePlanAgentsConfiguration = new MigratePlanAgentsConfiguration(collectionFactory, null);
			migratePlanAgentsConfiguration.runUpgradeScript();
			Plan newPlan = actualPlans.find(Filters.empty(), null, null, null, 0).findFirst().orElseThrow(() -> new RuntimeException("No plans found in collection"));
			PlanAgentsConfiguration agents = newPlan.getAgents();
			assertTrue(agents instanceof PlanAgentPoolsConfiguration);
			PlanAgentPoolsConfiguration agentsPoolsConfiguration = (PlanAgentPoolsConfiguration) agents;
			assertTrue(agentsPoolsConfiguration.configuredAgentPools.isEmpty());
		}
	}


	@Test
	public void testAutoScalingAutoConfig() throws IOException {
		try (InputStream is = this.getClass().getResourceAsStream("oldPlanWithAutoscalingSettingsAuto.json")) {
			ObjectMapper mapper = DefaultJacksonMapperProvider.getObjectMapper();
			Document oldPlan = mapper.readValue(is, Document.class);
			InMemoryCollectionFactory collectionFactory = new InMemoryCollectionFactory(new Properties());
			Collection<Document> plans = collectionFactory.getCollection("plans", step.core.collections.Document.class);
			Collection<Plan> actualPlans = collectionFactory.getCollection("plans", Plan.class);
			plans.save(oldPlan);
			MigratePlanAgentsConfiguration migratePlanAgentsConfiguration = new MigratePlanAgentsConfiguration(collectionFactory, null);
			migratePlanAgentsConfiguration.runUpgradeScript();
			Plan newPlan = actualPlans.find(Filters.empty(), null, null, null, 0).findFirst().orElseThrow(() -> new RuntimeException("No plans found in collection"));
			PlanAgentsConfiguration agents = newPlan.getAgents();
			assertTrue(agents instanceof PlanAgentsPoolsAutoConfiguration);
			assertEquals(((PlanAgentsPoolsAutoConfiguration) agents).mode, PlanAgentsPoolsAutoConfiguration.PlanAgentsPoolAutoMode.auto_detect);
		}
	}

	@Test
	public void testAutoScalingManualConfig() throws IOException {
		try (InputStream is = this.getClass().getResourceAsStream("oldPlanWithAutoscalingSettingsManual.json")) {
			ObjectMapper mapper = DefaultJacksonMapperProvider.getObjectMapper();
			Document oldPlan = mapper.readValue(is, Document.class);
			InMemoryCollectionFactory collectionFactory = new InMemoryCollectionFactory(new Properties());
			Collection<Document> plans = collectionFactory.getCollection("plans", step.core.collections.Document.class);
			Collection<Plan> actualPlans = collectionFactory.getCollection("plans", Plan.class);
			plans.save(oldPlan);
			MigratePlanAgentsConfiguration migratePlanAgentsConfiguration = new MigratePlanAgentsConfiguration(collectionFactory, null);
			migratePlanAgentsConfiguration.runUpgradeScript();
			Plan newPlan = actualPlans.find(Filters.empty(), null, null, null, 0).findFirst().orElseThrow(() -> new RuntimeException("No plans found in collection"));
			PlanAgentsConfiguration agents = newPlan.getAgents();
			assertTrue(agents instanceof PlanAgentPoolsConfiguration);
			PlanAgentPoolsConfiguration agentsPoolsConfiguration = (PlanAgentPoolsConfiguration) agents;
			assertEquals(4, agentsPoolsConfiguration.configuredAgentPools.size());
			List<AgentPoolConfiguration> expectedPools = List.of(new AgentPoolConfiguration("build-high-cpu-enterprise-agent", null, 1),
					new AgentPoolConfiguration("git-enterprise-agent", null, 2),
					new AgentPoolConfiguration("build-enterprise-agent", null, 1),
					new AgentPoolConfiguration("test-enterprise-agent", null, 1));
			assertTrue(CollectionUtils.isEqualCollection(expectedPools, agentsPoolsConfiguration.configuredAgentPools));
		}
	}

}
