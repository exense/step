/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.agents.provisioning.local;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.core.agents.AgentTypeConstants;
import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.core.agents.provisioning.TokenSelectionCriteriaFilter;
import step.core.execution.ProvisioningException;
import step.core.plans.agents.configuration.AgentPoolProvisioningConfiguration;
import step.core.plans.agents.configuration.ManualAgentProvisioningConfiguration;
import step.grid.agent.AgentTypes;
import step.grid.tokenpool.Interest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Covers how the forecast of an execution is turned into the size of the agents started for it. None of these tests
 * starts an agent: the grid is created, the sizing is a pure calculation on the requirements the forecasting produced.
 */
public class LocalAgentSizingTest {

    @Rule
    public final TemporaryFolder workDirectory = new TemporaryFolder();

    private static final String JAVA_POOL = "local-" + AgentTypeConstants.AGENT_TYPE_JAVA;

    /**
     * An agent of a local pool provides one token, so the forecast arrives here as the number of tokens the execution
     * needs and the agent is sized on it.
     */
    @Test
    public void sizesTheAgentOnTheForecast() throws Exception {
        withDriver(10, driver -> Assert.assertEquals(Map.of(AgentTypeConstants.AGENT_TYPE_JAVA, 3),
            driver.calculateTokensByAgentType(List.of(new AgentPoolRequirementSpec(JAVA_POOL, 3)))));
    }

    /**
     * All the tokens of an agent type are served by a single process, whatever the number of requirements they come
     * from.
     */
    @Test
    public void sumsTheRequirementsOfTheSameAgentType() throws Exception {
        withDriver(10, driver -> Assert.assertEquals(Map.of(AgentTypeConstants.AGENT_TYPE_JAVA, 5),
            driver.calculateTokensByAgentType(List.of(
                new AgentPoolRequirementSpec(JAVA_POOL, 2),
                new AgentPoolRequirementSpec(JAVA_POOL, 3)))));
    }

    @Test
    public void capsTheNumberOfTokensToTheConfiguredMaximum() throws Exception {
        withDriver(5, driver -> Assert.assertEquals(Map.of(AgentTypeConstants.AGENT_TYPE_JAVA, 5),
            driver.calculateTokensByAgentType(List.of(new AgentPoolRequirementSpec(JAVA_POOL, 12)))));
    }

    /**
     * An agent with no token would never become usable, and the execution would wait for it until it times out.
     */
    @Test
    public void alwaysSizesTheAgentWithAtLeastOneToken() throws Exception {
        withDriver(5, driver -> Assert.assertEquals(Map.of(AgentTypeConstants.AGENT_TYPE_JAVA, 1),
            driver.calculateTokensByAgentType(List.of(new AgentPoolRequirementSpec(JAVA_POOL, 0)))));
    }

    /**
     * The pools of a real Step instance, and the pools of agent types this distribution does not ship, are rejected
     * rather than silently served by whatever is available.
     */
    @Test
    public void rejectsAPoolWhichIsNotAvailableLocally() throws Exception {
        withDriver(5, driver -> {
            ProvisioningException exception = Assert.assertThrows(ProvisioningException.class,
                () -> driver.calculateTokensByAgentType(List.of(new AgentPoolRequirementSpec("windows-medium", 1))));
            Assert.assertTrue("Should list the available pools: " + exception.getMessage(),
                exception.getMessage().contains(JAVA_POOL));
        });
    }

    /**
     * A plan configuring its agent pools manually names the pools of a Step instance, which don't exist locally: it gets
     * one agent per agent type of the forecast, with the maximum number of tokens.
     */
    @Test
    public void startsOneAgentPerForecastedAgentTypeForConfiguredAgentPools() throws Exception {
        ManualAgentProvisioningConfiguration manualAgentProvisioningConfiguration = new ManualAgentProvisioningConfiguration();
        manualAgentProvisioningConfiguration.configuredAgentPools = List.of(new AgentPoolProvisioningConfiguration("windows-medium", null, 2));
        withDriver(5, driver -> {
            List<AgentPoolRequirementSpec> requirements = driver.resolveConfiguredAgentPools(
                manualAgentProvisioningConfiguration,
                List.of(new AgentPoolRequirementSpec(JAVA_POOL, 1), new AgentPoolRequirementSpec(JAVA_POOL, 3)),
                Set.of());
            Assert.assertEquals(1, requirements.size());
            Assert.assertEquals(JAVA_POOL, requirements.get(0).agentPoolTemplateName);
            Assert.assertEquals(5, requirements.get(0).numberOfAgents);
        });
    }

    /**
     * A plan configuring no agent pool at all disables the provisioning to run on permanent agents, which don't exist
     * locally either: it gets the same agents as a plan configuring its agent pools manually.
     */
    @Test
    public void startsOneAgentPerForecastedAgentTypeForAPlanDisablingTheProvisioning() throws Exception {
        ManualAgentProvisioningConfiguration manualAgentProvisioningConfiguration = new ManualAgentProvisioningConfiguration();
        manualAgentProvisioningConfiguration.configuredAgentPools = List.of();
        withDriver(5, driver -> {
            List<AgentPoolRequirementSpec> requirements = driver.resolveConfiguredAgentPools(manualAgentProvisioningConfiguration,
                List.of(new AgentPoolRequirementSpec(JAVA_POOL, 2)), Set.of());
            Assert.assertEquals(List.of(new AgentPoolRequirementSpec(JAVA_POOL, 5)), requirements);
        });
    }

    @Test
    public void startsNoAgentForConfiguredAgentPoolsWhenNoKeywordRequiresOne() throws Exception {
        ManualAgentProvisioningConfiguration manualAgentProvisioningConfiguration = new ManualAgentProvisioningConfiguration();
        manualAgentProvisioningConfiguration.configuredAgentPools = List.of(new AgentPoolProvisioningConfiguration("windows-medium", null, 2));
        withDriver(5, driver -> Assert.assertEquals(List.of(), driver.resolveConfiguredAgentPools(
            manualAgentProvisioningConfiguration, List.of(), Set.of())));
    }

    @Test
    public void rejectsConfiguredAgentPoolsWhenARequiredAgentTypeIsNotAvailable() throws Exception {
        ManualAgentProvisioningConfiguration manualAgentProvisioningConfiguration = new ManualAgentProvisioningConfiguration();
        manualAgentProvisioningConfiguration.configuredAgentPools = List.of(new AgentPoolProvisioningConfiguration("windows-medium", null, 2));
        withDriver(5, driver -> {
            ProvisioningException exception = Assert.assertThrows(ProvisioningException.class,
                () -> driver.resolveConfiguredAgentPools(manualAgentProvisioningConfiguration, List.of(),
                    Set.of(Map.of(AgentTypes.AGENT_TYPE_KEY, new Interest(Pattern.compile(AgentTypeConstants.AGENT_TYPE_DOTNET), true)))));
            Assert.assertEquals("This plan requires agent types which are not available for local execution: "
                + AgentTypeConstants.AGENT_TYPE_DOTNET + ".", exception.getMessage());
        });
    }

    /**
     * The filter keeps track of the criteria it already reported, which is per execution.
     */
    @Test
    public void createsATokenSelectionCriteriaFilterPerExecution() throws Exception {
        withDriver(5, driver -> {
            TokenSelectionCriteriaFilter filter = driver.createTokenSelectionCriteriaFilter();
            Assert.assertTrue(filter instanceof LocalTokenSelectionCriteriaFilter);
            Assert.assertNotSame(filter, driver.createTokenSelectionCriteriaFilter());
        });
    }

    /**
     * Runs the given assertions on a driver whose only available agent type is Java, which keeps these tests
     * independent from what the machine running them has installed.
     */
    private void withDriver(int maxTokensPerAgent, DriverAssertions assertions) throws Exception {
        LocalAgentProvisioningConfiguration configuration = new LocalAgentProvisioningConfiguration()
            .setMaxTokensPerAgent(maxTokensPerAgent)
            .setWorkDirectory(workDirectory.getRoot().toPath());
        LocalAgentWorkspace workspace = new LocalAgentWorkspace(configuration.getWorkDirectory());
        try (LocalExecutionGrid grid = LocalExecutionGrid.startEmbedded(configuration.getAgentStartTimeout(), workspace);
             LocalProcessAgentProvisioningDriver driver = new LocalProcessAgentProvisioningDriver(grid, workspace,
                 configuration, List.of(new JavaLocalAgentProvider(configuration, workspace)))) {
            Assert.assertEquals("Only the Java agent must be available in this test",
                Set.of(AgentTypeConstants.AGENT_TYPE_JAVA), driver.getAvailableAgentTypes());
            assertions.check(driver);
        }
    }

    private interface DriverAssertions {
        void check(LocalProcessAgentProvisioningDriver driver) throws IOException;
    }
}
