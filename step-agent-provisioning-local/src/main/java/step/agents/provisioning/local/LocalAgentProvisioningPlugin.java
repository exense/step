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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.handlers.functions.AgentProvisioningExecutionPlugin;
import step.core.agents.provisioning.driver.AgentProvisioningDriver;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.FunctionPlugin;
import step.grid.Grid;
import step.grid.client.GridClient;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

/**
 * Sets up the keywords of a local execution to run on real agents started on the developer machine.
 * <p>
 * This is the local equivalent of what the controller does with its grid and its agent provisioning driver: it starts
 * an embedded grid and publishes it and a {@link LocalProcessAgentProvisioningDriver} in the engine context, so that
 * the function, token forecasting and {@link AgentProvisioningExecutionPlugin agent provisioning} plugins pick them up.
 * The latter has to be added to the execution engine as well: it is the one provisioning the agents an execution needs.
 * <p>
 * It is deliberately <b>not</b> auto-discovered: the JUnit runner, where
 * running keywords in the same JVM is a feature rather than a limitation, must keep the in-JVM path.
 * <p>
 * Unlike most plugins it is {@link Closeable}, and has to be closed by whoever built it, after the execution engine it
 * was given to. See {@link #close()}.
 */
// Runs before FunctionPlugin, which builds the function execution service around whichever grid client it finds in
// the context, and before AgentProvisioningExecutionPlugin, which requires the driver: both have to be published
// before that. Neither knows anything about this plugin, hence runsBefore rather than dependencies declared on their side.
@Plugin(runsBefore = {FunctionPlugin.class, AgentProvisioningExecutionPlugin.class})
@IgnoreDuringAutoDiscovery
public class LocalAgentProvisioningPlugin extends AbstractExecutionEnginePlugin implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(LocalAgentProvisioningPlugin.class);

    private final LocalAgentProvisioningConfiguration configuration;
    private LocalExecutionGrid grid;
    private boolean closed;

    public LocalAgentProvisioningPlugin() {
        this(new LocalAgentProvisioningConfiguration());
    }

    public LocalAgentProvisioningPlugin(LocalAgentProvisioningConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
    }

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
        if (closed) {
            // Would otherwise publish the grid client of a grid which has been stopped, and fail every keyword later
            throw new PluginCriticalException("This local agent provisioning plugin has been closed and cannot be reused");
        }
        // Failures here are raised as PluginCriticalException, carrying on would leave the engine without a grid and without a
        // provisioning driver, and every plan would then fail with a misleading "no agent available for local
        // execution" instead of with the actual reason the local execution could not be set up.
        LocalAgentWorkspace workspace;
        try {
            workspace = new LocalAgentWorkspace(configuration.getWorkDirectory());
        } catch (IOException e) {
            throw new PluginCriticalException("Error while creating the working directory of the local agents", e);
        }

        try {
            grid = LocalExecutionGrid.startEmbedded(configuration.getAgentStartTimeout(), workspace);
        } catch (Exception e) {
            throw new PluginCriticalException("Error while starting the local grid", e);
        }

        LocalProcessAgentProvisioningDriver driver;
        try {
            driver = LocalAgentProvisioning.createDriver(grid, workspace, configuration);
        } catch (RuntimeException e) {
            closeQuietly(grid);
            grid = null;
            throw new PluginCriticalException("Error while initializing the local agents", e);
        }

        // Picked up by FunctionPlugin (grid client), TokenForecastingExecutionPlugin and AgentProvisioningExecutionPlugin
        // (driver). Both are Closeable and are closed by the context when the execution engine is closed: the grid
        // client releases the class loaders of the local tokens and the driver stops any agent still running. The grid
        // itself is deliberately not registered here, see close().
        context.put(Grid.class, grid.getGrid());
        context.put(GridClient.class, grid.getGridClient());
        context.put(AgentProvisioningDriver.class, driver);
    }

    /**
     * Stops the local grid and deletes the files its file manager cached.
     * <p>
     * Done here rather than by registering the grid in the execution engine context, because the context closes what
     * it holds in no particular order, while this has to happen <b>last</b>: the local tokens of the grid client load
     * their handlers (the composite handler of every plan calling a composite keyword, for one) with class loaders
     * reading the jars straight out of the file manager directory of this grid. Deleting it before the grid client
     * released them leaves the files open, and Windows refuses to delete an open file.
     * <p>
     * The caller closes this plugin after the execution engine it was given to, typically by declaring it as the first
     * resource of the same try-with-resources.
     */
    @Override
    public void close() throws IOException {
        closed = true;
        if (grid != null) {
            grid.close();
            grid = null;
        }
    }

    private static void closeQuietly(LocalExecutionGrid grid) {
        try {
            grid.close();
        } catch (IOException e) {
            logger.warn("Error while stopping the local grid after a failed initialization", e);
        }
    }
}
