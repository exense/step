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
package step.cli.local;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.core.agents.AgentTypeConstants;
import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.core.execution.ProvisioningException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * Runs the given assertions on a driver whose only available agent type is Java, which keeps these tests
     * independent from what the machine running them has installed.
     */
    private void withDriver(int maxTokensPerAgent, DriverAssertions assertions) throws Exception {
        LocalAgentProvisioningConfiguration configuration = new LocalAgentProvisioningConfiguration()
            .setMaxTokensPerAgent(maxTokensPerAgent)
            .setWorkDirectory(workDirectory.getRoot().toPath());
        LocalAgentWorkspace workspace = new LocalAgentWorkspace(configuration.getWorkDirectory());
        try (LocalExecutionGrid grid = new LocalExecutionGrid(configuration.getAgentStartTimeout(), workspace);
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
