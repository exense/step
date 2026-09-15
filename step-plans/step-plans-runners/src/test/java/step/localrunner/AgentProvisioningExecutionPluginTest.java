/*******************************************************************************
 * Copyright (C) exense GmbH
 ******************************************************************************/
package step.localrunner;

import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.handlers.functions.AgentProvisioningExecutionPlugin;
import step.artefacts.handlers.functions.TokenForecastingExecutionPlugin;
import step.artefacts.handlers.functions.test.MyFunction;
import step.artefacts.handlers.functions.test.MyFunctionType;
import step.core.accessors.AbstractOrganizableObject;
import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.core.agents.provisioning.AgentPoolSpec;
import step.core.agents.provisioning.TokenSelectionCriteriaFilter;
import step.core.agents.provisioning.driver.AgentProvisioningDriver;
import step.core.agents.provisioning.driver.AgentProvisioningDriverConfiguration;
import step.core.agents.provisioning.driver.AgentProvisioningRequest;
import step.core.agents.provisioning.driver.AgentProvisioningStatus;
import step.core.agents.provisioning.driver.AgentProvisioningStatusAccessor;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.plans.Plan;
import step.core.plans.agents.configuration.AgentPoolProvisioningConfiguration;
import step.core.plans.agents.configuration.ManualAgentProvisioningConfiguration;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.core.plugins.exceptions.PluginCriticalException;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.BasePlugin;
import step.engine.plugins.FunctionPlugin;
import step.functions.io.Output;
import step.functions.type.FunctionTypeRegistry;
import step.grid.tokenpool.Interest;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.threadpool.ThreadPoolPlugin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static step.core.agents.provisioning.driver.AgentProvisioningStatus.AGENT_PROVISIONING_STATUS_ID_CUSTOM_FIELD;

public class AgentProvisioningExecutionPluginTest {

    private static final String POOL = "pool1";

    @Test
    public void failsWithoutDriver() {
        assertThrows(PluginCriticalException.class, () -> ExecutionEngine.builder()
            .withPlugin(new AgentProvisioningExecutionPlugin(new AgentProvisioningStatusAccessor(new InMemoryCollection<>()))).build());
    }

    @Test
    public void provisionsTheForecastAndPersistsTheStatus() {
        RecordingDriver driver = new RecordingDriver(Map.of("$agenttype", "default"));
        AgentProvisioningStatusAccessor accessor = new AgentProvisioningStatusAccessor(new InMemoryCollection<>());

        try (ExecutionEngine executionEngine = newExecutionEngine(driver, accessor)) {
            PlanRunnerResult result = executionEngine.execute(plan());

            assertEquals(ReportNodeStatus.PASSED, result.getResult());
            assertEquals(List.of(new AgentPoolRequirementSpec(POOL, 1)), driver.request.agentPoolRequirementSpecs);
            assertEquals(1, driver.deprovisionCount.get());
            AgentProvisioningStatus persistedStatus = accessor.getAll().next();
            assertEquals(result.getExecutionId(), persistedStatus.executionId);
            assertEquals(persistedStatus.getId().toHexString(), executionEngine.getExecutionEngineContext().getExecutionAccessor()
                .get(result.getExecutionId()).getCustomField(AGENT_PROVISIONING_STATUS_ID_CUSTOM_FIELD));
            // The filter of the driver is applied to the token selection criteria of the keywords
            assertTrue(driver.filterInvocations.get() > 0);
        }
    }

    @Test
    public void reportsTheCriteriaWithoutMatchWithTheMessageOfTheDriver() {
        RecordingDriver driver = new RecordingDriver(Map.of("$agenttype", "unknown"));

        try (ExecutionEngine executionEngine = newExecutionEngine(driver, new AgentProvisioningStatusAccessor(new InMemoryCollection<>()))) {
            PlanRunnerResult result = executionEngine.execute(plan());

            assertEquals(ReportNodeStatus.TECHNICAL_ERROR, result.getResult());
            assertEquals("Unmatched: [{$agenttype=default}]", result.getErrorSummary());
            assertNull(driver.request);
        }
    }

    @Test
    public void letsTheDriverResolveConfiguredAgentPools() {
        RecordingDriver driver = new RecordingDriver(Map.of("$agenttype", "default"));
        Plan plan = plan();
        AgentPoolProvisioningConfiguration configuredPool = new AgentPoolProvisioningConfiguration();
        configuredPool.pool = "windows-medium";
        configuredPool.replicas = 3;
        ManualAgentProvisioningConfiguration agentConfiguration = new ManualAgentProvisioningConfiguration();
        agentConfiguration.configuredAgentPools = List.of(configuredPool);
        plan.setAgents(agentConfiguration);

        try (ExecutionEngine executionEngine = newExecutionEngine(driver, new AgentProvisioningStatusAccessor(new InMemoryCollection<>()))) {
            PlanRunnerResult result = executionEngine.execute(plan);

            assertEquals(ReportNodeStatus.PASSED, result.getResult());
            assertEquals(List.of(new AgentPoolRequirementSpec("windows-medium", 3)), driver.configuredAgentPools);
            assertEquals(List.of(new AgentPoolRequirementSpec(POOL, 1)), driver.forecastedAgentPools);
            assertEquals(RecordingDriver.RESOLVED_AGENT_POOLS, driver.request.agentPoolRequirementSpecs);
        }
    }

    private static Plan plan() {
        Plan plan = PlanBuilder.create()
            .startBlock(BaseArtefacts.testCase())
            .add(FunctionArtefacts.keyword("test"))
            .endBlock().build();
        MyFunction function = new MyFunction(input -> new Output<>());
        function.addAttribute(AbstractOrganizableObject.NAME, "test");
        plan.setFunctions(List.of(function));
        return plan;
    }

    private static ExecutionEngine newExecutionEngine(AgentProvisioningDriver driver, AgentProvisioningStatusAccessor accessor) {
        ExecutionEngineContext parentContext = new ExecutionEngineContext(step.core.execution.OperationMode.LOCAL_PLAN, true);
        parentContext.put(AgentProvisioningDriver.class, driver);
        return ExecutionEngine.builder()
            .withParentContext(parentContext)
            .withPlugin(new BasePlugin())
            .withPlugin(new FunctionPlugin())
            .withPlugin(new AbstractExecutionEnginePlugin() {
                @Override
                public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
                    executionContext.require(FunctionTypeRegistry.class).registerFunctionType(new MyFunctionType());
                }
            })
            .withPlugin(new ThreadPoolPlugin())
            .withPlugin(new BaseArtefactPlugin())
            .withPlugin(new TokenForecastingExecutionPlugin())
            .withPlugin(new AgentProvisioningExecutionPlugin(accessor))
            .build();
    }

    private static class RecordingDriver implements AgentProvisioningDriver {

        static final List<AgentPoolRequirementSpec> RESOLVED_AGENT_POOLS = List.of(new AgentPoolRequirementSpec(POOL, 5));

        private final AgentProvisioningDriverConfiguration configuration;
        private final Map<String, AgentProvisioningStatus> statuses = new java.util.concurrent.ConcurrentHashMap<>();
        private final AtomicInteger deprovisionCount = new AtomicInteger();
        private final AtomicInteger filterInvocations = new AtomicInteger();
        private AgentProvisioningRequest request;
        private List<AgentPoolRequirementSpec> configuredAgentPools;
        private List<AgentPoolRequirementSpec> forecastedAgentPools;

        RecordingDriver(Map<String, String> poolAttributes) {
            configuration = new AgentProvisioningDriverConfiguration();
            configuration.availableAgentPools = Set.of(new AgentPoolSpec(POOL, poolAttributes, 1));
        }

        @Override
        public AgentProvisioningDriverConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        public String initializeTokenProvisioningRequest(AgentProvisioningRequest request) {
            this.request = request;
            String id = UUID.randomUUID().toString();
            statuses.put(id, new AgentProvisioningStatus());
            return id;
        }

        @Override
        public AgentProvisioningStatus executeTokenProvisioningRequest(String provisioningRequestId) {
            return statuses.get(provisioningRequestId);
        }

        @Override
        public AgentProvisioningStatus getTokenProvisioningStatus(String provisioningRequestId) {
            return statuses.get(provisioningRequestId);
        }

        @Override
        public void deprovisionTokens(String provisioningRequestId) {
            deprovisionCount.incrementAndGet();
        }

        @Override
        public void registerRemoteAgentPoolSpecs(Set<AgentPoolSpec> agentPoolSpecs) {
        }

        @Override
        public List<AgentPoolRequirementSpec> resolveConfiguredAgentPools(List<AgentPoolRequirementSpec> configured,
                                                                          List<AgentPoolRequirementSpec> forecasted,
                                                                          Set<Map<String, Interest>> criteriaWithoutMatch) {
            configuredAgentPools = configured;
            forecastedAgentPools = forecasted;
            return RESOLVED_AGENT_POOLS;
        }

        @Override
        public String getUnmatchedCriteriaMessage(Set<Map<String, Interest>> criteriaWithoutMatch) {
            return "Unmatched: " + criteriaWithoutMatch;
        }

        @Override
        public TokenSelectionCriteriaFilter createTokenSelectionCriteriaFilter() {
            return criteria -> {
                filterInvocations.incrementAndGet();
                return criteria;
            };
        }
    }
}
