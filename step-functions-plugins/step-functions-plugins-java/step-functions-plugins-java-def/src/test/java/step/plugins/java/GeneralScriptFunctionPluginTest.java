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
package step.plugins.java;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.core.agents.provisioning.AgentPoolSpec;
import step.core.agents.provisioning.driver.AgentProvisioningDriver;
import step.core.agents.provisioning.driver.AgentProvisioningDriverConfiguration;
import step.core.agents.provisioning.driver.AgentProvisioningRequest;
import step.core.agents.provisioning.driver.AgentProvisioningStatus;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * The script engines are sent to the agents only when the driver provisioning them starts them from this application,
 * whatever the operation mode: the IDE runs its executions as a controller does.
 */
public class GeneralScriptFunctionPluginTest {

    private static final String GROOVY_LIBS = "plugins.groovy.libs";

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    /**
     * The IDE: the driver is the one of the controller, in the parent context
     */
    @Test
    public void declaresTheLibrariesForADriverOfTheParentContext() throws Exception {
        ExecutionEngineContext parentContext = new ExecutionEngineContext(OperationMode.CONTROLLER, true);
        parentContext.put(AgentProvisioningDriver.class, new TestDriver(folder.getRoot().toPath()));
        ExecutionEngineContext context = new ExecutionEngineContext(OperationMode.CONTROLLER, true);

        new GeneralScriptFunctionPlugin().initializeExecutionEngineContext(parentContext, context);

        String libraries = context.getConfiguration().getProperty(GROOVY_LIBS, null);
        Assert.assertNotNull(libraries);
        try (var files = Files.list(Path.of(libraries))) {
            Assert.assertTrue(files.anyMatch(file -> file.getFileName().toString().startsWith("groovy-")));
        }
    }

    /**
     * The CLI: the driver is published by a plugin of the same engine
     */
    @Test
    public void declaresTheLibrariesForADriverOfTheEngineContext() {
        ExecutionEngineContext context = new ExecutionEngineContext(OperationMode.CONTROLLER, true);
        context.put(AgentProvisioningDriver.class, new TestDriver(folder.getRoot().toPath()));

        new GeneralScriptFunctionPlugin().initializeExecutionEngineContext(null, context);

        Assert.assertNotNull(context.getConfiguration().getProperty(GROOVY_LIBS, null));
    }

    /**
     * The JUnit runners, and a controller without agent provisioning
     */
    @Test
    public void declaresNothingWithoutDriver() {
        ExecutionEngineContext context = new ExecutionEngineContext(OperationMode.CONTROLLER, true);

        new GeneralScriptFunctionPlugin().initializeExecutionEngineContext(null, context);

        Assert.assertNull(context.getConfiguration().getProperty(GROOVY_LIBS, null));
    }

    /**
     * A controller whose agents run elsewhere, and get the engines from its configuration
     */
    @Test
    public void declaresNothingForAgentsNotRunningFromThisApplication() {
        ExecutionEngineContext context = new ExecutionEngineContext(OperationMode.CONTROLLER, true);
        context.put(AgentProvisioningDriver.class, new TestDriver(null));

        new GeneralScriptFunctionPlugin().initializeExecutionEngineContext(null, context);

        Assert.assertNull(context.getConfiguration().getProperty(GROOVY_LIBS, null));
    }

    @Test
    public void keepsTheConfiguredLibraries() {
        ExecutionEngineContext context = new ExecutionEngineContext(OperationMode.CONTROLLER, true);
        context.getConfiguration().putProperty(GROOVY_LIBS, "configured");
        context.put(AgentProvisioningDriver.class, new TestDriver(folder.getRoot().toPath()));

        new GeneralScriptFunctionPlugin().initializeExecutionEngineContext(null, context);

        Assert.assertEquals("configured", context.getConfiguration().getProperty(GROOVY_LIBS, null));
    }

    /**
     * A driver installing the libraries of its agents under the given root, or not running them from this application
     * if it is null
     */
    private static class TestDriver implements AgentProvisioningDriver {

        private final Path root;

        private TestDriver(Path root) {
            this.root = root;
        }

        @Override
        public Path getLocalLibrariesDirectory() {
            return root;
        }

        @Override
        public AgentProvisioningDriverConfiguration getConfiguration() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String initializeTokenProvisioningRequest(AgentProvisioningRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AgentProvisioningStatus executeTokenProvisioningRequest(String provisioningRequestId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AgentProvisioningStatus getTokenProvisioningStatus(String provisioningRequestId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deprovisionTokens(String provisioningRequestId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void registerRemoteAgentPoolSpecs(Set<AgentPoolSpec> agentPoolSpecs) {
            throw new UnsupportedOperationException();
        }
    }
}
