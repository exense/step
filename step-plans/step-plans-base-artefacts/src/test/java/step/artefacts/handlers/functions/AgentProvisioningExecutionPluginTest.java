/*******************************************************************************
 * Copyright (C) exense GmbH
 ******************************************************************************/
package step.artefacts.handlers.functions;

import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
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
import step.core.execution.OperationMode;
import step.core.execution.ProvisioningException;
import step.core.plans.Plan;
import step.core.plans.agents.configuration.AgentPoolProvisioningConfiguration;
import step.core.plans.agents.configuration.AgentProvisioningConfiguration;
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
import java.util.concurrent.ConcurrentHashMap;
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

    /**
     * The contract of {@link AgentProvisioningDriver#getTokenProvisioningStatus} allows null, for a request the driver
     * no longer knows. The drivers of Step keep their request until it is deprovisioned, but a driver dropping it
     * once the provisioning is over must not fail an execution whose agents were provisioned.
     */
    @Test
    public void provisionsWithoutPersistingWhenTheDriverReturnsNoStatus() {
        RecordingDriver driver = new NoStatusDriver(null);
        AgentProvisioningStatusAccessor accessor = new AgentProvisioningStatusAccessor(new InMemoryCollection<>());

        try (ExecutionEngine executionEngine = newExecutionEngine(driver, accessor)) {
            PlanRunnerResult result = executionEngine.execute(plan());

            assertEquals(ReportNodeStatus.PASSED, result.getResult());
            assertFalse(accessor.getAll().hasNext());
            assertNull(executionEngine.getExecutionEngineContext().getExecutionAccessor()
                .get(result.getExecutionId()).getCustomField(AGENT_PROVISIONING_STATUS_ID_CUSTOM_FIELD));
        }
    }

    /**
     * The same, for a driver dropping its request when the provisioning fails: the error reported is the one of the
     * provisioning, not one raised while persisting its status.
     */
    @Test
    public void reportsTheProvisioningErrorWhenTheDriverReturnsNoStatus() {
        RecordingDriver driver = new NoStatusDriver(new ProvisioningException("Unable to start the agents"));

        try (ExecutionEngine executionEngine = newExecutionEngine(driver, new AgentProvisioningStatusAccessor(new InMemoryCollection<>()))) {
            PlanRunnerResult result = executionEngine.execute(plan());

            assertEquals(ReportNodeStatus.TECHNICAL_ERROR, result.getResult());
            assertEquals("Unable to start the agents", result.getErrorSummary());
        }
    }

    /**
     * The keyword requires the default agent type, which none of the agent pools of the driver provides
     */
    @Test
    public void reportsTheCriteriaWithoutMatch() {
        RecordingDriver driver = new RecordingDriver(Map.of("$agenttype", "unknown"));

        try (ExecutionEngine executionEngine = newExecutionEngine(driver, new AgentProvisioningStatusAccessor(new InMemoryCollection<>()))) {
            PlanRunnerResult result = executionEngine.execute(plan());

            assertEquals(ReportNodeStatus.TECHNICAL_ERROR, result.getResult());
            assertEquals("Some keywords of this plan cannot be executed: no agent pool matches their token selection"
                + " criteria {$agenttype=default}. Check the agent pools available for the agent provisioning, and the"
                + " token selection criteria of these keywords.", result.getErrorSummary());
            assertNull(driver.request);
        }
    }

    /**
     * A driver can phrase the error in its own terms: the local one names the agent types missing on this machine
     */
    @Test
    public void reportsTheCriteriaWithoutMatchWithTheMessageOfTheDriver() {
        RecordingDriver driver = new RecordingDriver(Map.of("$agenttype", "unknown")) {
            @Override
            public String getUnmatchedCriteriaMessage(Set<Map<String, Interest>> criteriaWithoutMatch) {
                return "No agent of type default on this machine";
            }
        };

        try (ExecutionEngine executionEngine = newExecutionEngine(driver, new AgentProvisioningStatusAccessor(new InMemoryCollection<>()))) {
            PlanRunnerResult result = executionEngine.execute(plan());

            assertEquals(ReportNodeStatus.TECHNICAL_ERROR, result.getResult());
            assertEquals("No agent of type default on this machine", result.getErrorSummary());
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

    /**
     * {@code agents: []} disables the provisioning to run on permanent agents. A driver without such agents, as the
     * local one, still gets to provision the agents the plan needs.
     */
    @Test
    public void letsTheDriverProvisionAPlanDisablingTheProvisioning() {
        RecordingDriver driver = new RecordingDriver(Map.of("$agenttype", "default"));

        try (ExecutionEngine executionEngine = newExecutionEngine(driver, new AgentProvisioningStatusAccessor(new InMemoryCollection<>()))) {
            PlanRunnerResult result = executionEngine.execute(planDisablingTheProvisioning());

            assertEquals(ReportNodeStatus.PASSED, result.getResult());
            assertEquals(List.of(), driver.configuredAgentPools);
            assertEquals(List.of(new AgentPoolRequirementSpec(POOL, 1)), driver.forecastedAgentPools);
            assertEquals(RecordingDriver.RESOLVED_AGENT_POOLS, driver.request.agentPoolRequirementSpecs);
            assertEquals(1, driver.deprovisionCount.get());
        }
    }

    @Test
    public void provisionsNothingForAPlanDisablingTheProvisioningByDefault() {
        RecordingDriver driver = new ConfiguredAgentPoolsDriver();

        try (ExecutionEngine executionEngine = newExecutionEngine(driver, new AgentProvisioningStatusAccessor(new InMemoryCollection<>()))) {
            PlanRunnerResult result = executionEngine.execute(planDisablingTheProvisioning());

            assertEquals(ReportNodeStatus.PASSED, result.getResult());
            assertNull(driver.request);
            assertEquals(0, driver.deprovisionCount.get());
        }
    }

    private static Plan planDisablingTheProvisioning() {
        Plan plan = plan();
        ManualAgentProvisioningConfiguration agentConfiguration = new ManualAgentProvisioningConfiguration();
        agentConfiguration.configuredAgentPools = List.of();
        plan.setAgents(agentConfiguration);
        return plan;
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
        ExecutionEngineContext parentContext = new ExecutionEngineContext(OperationMode.LOCAL_PLAN, true);
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

    /**
     * A driver provisioning the configured agent pools as they are, as {@link AgentProvisioningDriver} does by default
     */
    private static class ConfiguredAgentPoolsDriver extends RecordingDriver {

        ConfiguredAgentPoolsDriver() {
            super(Map.of("$agenttype", "default"));
        }

        @Override
        public List<AgentPoolRequirementSpec> resolveConfiguredAgentPools(AgentProvisioningConfiguration agentProvisioningConfiguration,
                                                                          List<AgentPoolRequirementSpec> forecasted,
                                                                          Set<Map<String, Interest>> criteriaWithoutMatch) {
            return agentProvisioningConfiguration.getAgentPoolRequirementSpecs();
        }
    }

    /**
     * A driver which has no status to return for its provisioning requests, optionally failing the provisioning
     */
    private static class NoStatusDriver extends RecordingDriver {

        private final ProvisioningException provisioningError;

        NoStatusDriver(ProvisioningException provisioningError) {
            super(Map.of("$agenttype", "default"));
            this.provisioningError = provisioningError;
        }

        @Override
        public AgentProvisioningStatus executeTokenProvisioningRequest(String provisioningRequestId) {
            if (provisioningError != null) {
                throw provisioningError;
            }
            return super.executeTokenProvisioningRequest(provisioningRequestId);
        }

        @Override
        public AgentProvisioningStatus getTokenProvisioningStatus(String provisioningRequestId) {
            return null;
        }
    }

    private static class RecordingDriver implements AgentProvisioningDriver {

        static final List<AgentPoolRequirementSpec> RESOLVED_AGENT_POOLS = List.of(new AgentPoolRequirementSpec(POOL, 5));

        private final AgentProvisioningDriverConfiguration configuration;
        private final Map<String, AgentProvisioningStatus> statuses = new ConcurrentHashMap<>();
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
        public List<AgentPoolRequirementSpec> resolveConfiguredAgentPools(AgentProvisioningConfiguration agentProvisioningConfiguration,
                                                                          List<AgentPoolRequirementSpec> forecasted,
                                                                          Set<Map<String, Interest>> criteriaWithoutMatch) {
            configuredAgentPools = agentProvisioningConfiguration.getAgentPoolRequirementSpecs();
            forecastedAgentPools = forecasted;
            return RESOLVED_AGENT_POOLS;
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
