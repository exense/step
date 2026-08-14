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
import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.core.agents.provisioning.AgentPoolSpec;
import step.core.agents.provisioning.driver.AgentProvisioningRequest;
import step.core.agents.AgentTypeConstants;
import step.grid.agent.AgentTypes;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Starts a real Java agent as a separate process and checks that it joins the embedded grid.
 * <p>
 * The agent is the one embedded in the CLI jar, so this needs no external setup: the build copies the agent bundle
 * into the module's resources, which puts it on the test classpath too.
 */
public class LocalProcessAgentProvisioningDriverTest {

    @Rule
    public final TemporaryFolder workDirectory = new TemporaryFolder();

    @Test
    public void startsAndStopsAJavaAgent() throws Exception {
        LocalAgentProvisioningConfiguration configuration = new LocalAgentProvisioningConfiguration()
            .setMaxTokensPerAgent(2)
            .setWorkDirectory(workDirectory.getRoot().toPath())
            .setAgentStartTimeout(Duration.ofSeconds(90));

        LocalAgentWorkspace workspace = new LocalAgentWorkspace(configuration.getWorkDirectory());
        RecordingLocalAgentProvider provider =
            new RecordingLocalAgentProvider(new JavaLocalAgentProvider(configuration, workspace));
        Assert.assertTrue("The Java agent should be embedded in the CLI", provider.isAvailable());

        try (LocalExecutionGrid grid = new LocalExecutionGrid(configuration.getAgentStartTimeout(), workspace)) {
            try (LocalProcessAgentProvisioningDriver driver = new LocalProcessAgentProvisioningDriver(grid, workspace,
                configuration, List.of(provider, new UnavailableLocalAgentProvider()))) {

                Set<AgentPoolSpec> availablePools = driver.getConfiguration().availableAgentPools;
                Assert.assertEquals("An unavailable agent type is not offered", 1, availablePools.size());
                // The unavailable provider is nevertheless the one able to say what is missing for its agent type
                Assert.assertEquals(UnavailableLocalAgentProvider.INSTALLATION_HINT,
                    driver.getInstallationHint(UnavailableLocalAgentProvider.AGENT_TYPE));
                Assert.assertNull(driver.getInstallationHint("aTypeNoProviderKnows"));
                AgentPoolSpec javaPool = availablePools.iterator().next();
                Assert.assertEquals(Map.of(AgentTypes.AGENT_TYPE_KEY, AgentTypeConstants.AGENT_TYPE_JAVA), javaPool.attributes);
                // One token per pool agent is what makes the forecast reach the driver as a number of tokens
                Assert.assertEquals(1, javaPool.numberOfTokens);

                AgentProvisioningRequest request = new AgentProvisioningRequest();
                request.executionId = "testExecution";
                // Two agents of a pool of one token: one process with two tokens
                request.agentPoolRequirementSpecs = List.of(new AgentPoolRequirementSpec(javaPool.name, 2));

                String requestId = driver.initializeTokenProvisioningRequest(request);
                driver.executeTokenProvisioningRequest(requestId);

                // Provisioning only returns once every token has connected and the agent has answered a ping
                Assert.assertNull(driver.getTokenProvisioningStatus(requestId).error);
                Assert.assertEquals(2, grid.getGrid().getTokens().size());

                driver.deprovisionTokens(requestId);
                // What matters is that no agent survives the execution. The grid still lists its tokens at this
                // point: they are only dropped when the registration times out, which the CLI never waits for since
                // it tears the grid down right after.
                Assert.assertFalse("The agent process should have been stopped",
                    provider.getStartedProcess().isAlive());
            }
        }
    }

    /**
     * Delegates to the real provider and keeps hold of the started process, to assert on its lifecycle.
     */
    private static class RecordingLocalAgentProvider implements LocalAgentProvider {

        private final LocalAgentProvider delegate;
        private LocalAgentProcess startedProcess;

        RecordingLocalAgentProvider(LocalAgentProvider delegate) {
            this.delegate = delegate;
        }

        LocalAgentProcess getStartedProcess() {
            Assert.assertNotNull("No agent was started", startedProcess);
            return startedProcess;
        }

        @Override
        public String getAgentType() {
            return delegate.getAgentType();
        }

        @Override
        public String getDisplayName() {
            return delegate.getDisplayName();
        }

        @Override
        public boolean isAvailable() {
            return delegate.isAvailable();
        }

        @Override
        public LocalAgentProcess start(LocalAgentStartContext context) throws LocalAgentException {
            startedProcess = delegate.start(context);
            return startedProcess;
        }
    }

    /**
     * An agent type this machine cannot start, as the .NET one is without an installation to point at.
     */
    private static class UnavailableLocalAgentProvider implements LocalAgentProvider {

        static final String AGENT_TYPE = "anUnavailableType";
        static final String INSTALLATION_HINT = "Install what this agent type runs on.";

        @Override
        public String getAgentType() {
            return AGENT_TYPE;
        }

        @Override
        public String getDisplayName() {
            return "Unavailable";
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public String getInstallationHint() {
            return INSTALLATION_HINT;
        }

        @Override
        public LocalAgentProcess start(LocalAgentStartContext context) {
            throw new UnsupportedOperationException("An unavailable agent type is never started");
        }
    }
}
