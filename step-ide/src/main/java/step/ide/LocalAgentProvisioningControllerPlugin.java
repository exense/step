package step.ide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.agents.provisioning.local.LocalAgentProvisioning;
import step.agents.provisioning.local.LocalAgentProvisioningConfiguration;
import step.agents.provisioning.local.LocalAgentWorkspace;
import step.agents.provisioning.local.LocalExecutionGrid;
import step.artefacts.handlers.functions.AgentProvisioningExecutionPlugin;
import step.controller.grid.GridPlugin;
import step.core.GlobalContext;
import step.core.agents.provisioning.driver.AgentProvisioningDriver;
import step.core.agents.provisioning.driver.AgentProvisioningStatus;
import step.core.agents.provisioning.driver.AgentProvisioningStatusAccessor;
import step.core.collections.Collection;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.grid.GridImpl;
import step.grid.security.SymmetricSecurityConfiguration;

import java.io.IOException;

import static step.core.agents.provisioning.driver.AgentProvisioningStatus.AGENT_PROVISIONING_STATUS_ENTITY_NAME;

/**
 * Provisions the agents of the executions run from the IDE by starting them as separate processes on the developer
 * machine, like the local execution of the CLI does. Unlike the CLI, the agents register to the grid the IDE already
 * runs: it is the one the keywords of the executions are routed through.
 */
@Plugin(dependencies = {GridPlugin.class})
public class LocalAgentProvisioningControllerPlugin extends AbstractControllerPlugin {

    private static final Logger logger = LoggerFactory.getLogger(LocalAgentProvisioningControllerPlugin.class);

    private LocalExecutionGrid grid;
    private AgentProvisioningDriver driver;
    private AgentProvisioningStatusAccessor agentProvisioningStatusAccessor;

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        if (context.get(AgentProvisioningDriver.class) != null) {
            logger.info("An agent provisioning driver is already configured. The local agent provisioning is disabled.");
            return;
        }
        LocalAgentProvisioningConfiguration configuration = new LocalAgentProvisioningConfiguration();
        LocalAgentWorkspace workspace = new LocalAgentWorkspace(configuration.getWorkDirectory());

        grid = LocalExecutionGrid.attach(context.require(GridImpl.class), context.get(SymmetricSecurityConfiguration.class),
            configuration.getAgentStartTimeout());
        try {
            driver = LocalAgentProvisioning.createDriver(grid, workspace, configuration);
        } catch (RuntimeException e) {
            closeGridQuietly();
            throw e;
        }
        context.put(AgentProvisioningDriver.class, driver);

        Collection<AgentProvisioningStatus> agentProvisioningStatus = context.getCollectionFactory().getCollection(AGENT_PROVISIONING_STATUS_ENTITY_NAME, AgentProvisioningStatus.class);
        agentProvisioningStatus.createOrUpdateIndex("executionId");
        agentProvisioningStatusAccessor = new AgentProvisioningStatusAccessor(agentProvisioningStatus);
        context.put(AgentProvisioningStatusAccessor.class, agentProvisioningStatusAccessor);

        LocalAgentProvisioning.declareScriptEngineLibraries(context.getConfiguration(), workspace);
    }

    @Override
    public ExecutionEnginePlugin getExecutionEnginePlugin() {
        return driver != null ? new AgentProvisioningExecutionPlugin(agentProvisioningStatusAccessor) : null;
    }

    @Override
    public void serverStop(GlobalContext context) {
        if (driver != null) {
            // Stops the agents still running
            driver.close();
            driver = null;
        }
        closeGridQuietly();
    }

    private void closeGridQuietly() {
        if (grid != null) {
            try {
                grid.close();
            } catch (IOException e) {
                logger.warn("Error while closing the grid client of the local agent provisioning", e);
            }
            grid = null;
        }
    }
}
