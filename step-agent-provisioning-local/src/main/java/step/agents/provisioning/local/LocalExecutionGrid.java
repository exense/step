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

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.GridImpl;
import step.grid.client.GridClientConfiguration;
import step.grid.client.LocalGridClientImpl;
import step.grid.security.SymmetricSecurityConfiguration;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * The grid the local agents register to, embedded in the CLI.
 * <p>
 * It is the very same {@link GridImpl} a Step controller runs, started on an ephemeral port for the lifetime of one
 * CLI invocation. Reusing it rather than emulating it is what makes a local execution go through the same path as a
 * platform execution: the same token selection, the same file transfer to the agents, the same keyword protocol.
 */
public class LocalExecutionGrid implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(LocalExecutionGrid.class);
    private static final int SECRET_KEY_LENGTH_BYTES = 32;

    private final GridImpl grid;
    private final LocalGridClientImpl gridClient;
    private final SymmetricSecurityConfiguration security;
    private final Path fileManagerDirectory;

    /**
     * @param workspace the workspace the file manager of this grid caches its files in. Using it rather than a
     *                  temporary directory of its own is what gets that cache deleted: it is deleted with this grid,
     *                  and swept by the next run should this CLI be killed before it can do so.
     */
    public LocalExecutionGrid(Duration agentStartTimeout, LocalAgentWorkspace workspace) throws Exception {
        // The grid listens on all interfaces, so it is protected with a secret rather than left open to anything
        // running on the machine. The secret is generated per invocation and never leaves this process and the
        // configuration files of the agents it starts, both of which are gone when the CLI terminates.
        security = new SymmetricSecurityConfiguration(generateSecretKey());

        GridImpl.GridImplConfig gridConfig = new GridImpl.GridImplConfig();
        gridConfig.setSecurity(security);

        fileManagerDirectory = workspace.createGridRunDirectory();

        // Port 0: the OS assigns a free port, which keeps concurrent CLI invocations from colliding
        grid = new GridImpl(fileManagerDirectory.toFile(), 0, gridConfig);
        grid.start();
        logger.debug("Started the local grid on port {}", grid.getServerPort());

        gridClient = new LocalGridClientImpl(gridClientConfiguration(agentStartTimeout, security), grid);
    }

    private static GridClientConfiguration gridClientConfiguration(Duration agentStartTimeout, SymmetricSecurityConfiguration security) {
        GridClientConfiguration configuration = new GridClientConfiguration();
        // The same secret as the grid and the agents: without it this client could neither reserve tokens nor call
        // the very agents the CLI started.
        configuration.setGridSecurity(security);
        // Selecting a token has to wait for the agent process to start, which is the slowest part of a local
        // execution and dominated by the JVM start-up of the agent.
        configuration.setNoMatchExistsTimeout(agentStartTimeout.toMillis());
        configuration.setMatchExistsTimeout(agentStartTimeout.toMillis());
        configuration.setUseLocalAgentUrlIfAvailable(true);
        return configuration;
    }

    private static String generateSecretKey() {
        byte[] secret = new byte[SECRET_KEY_LENGTH_BYTES];
        new SecureRandom().nextBytes(secret);
        return Base64.getEncoder().encodeToString(secret);
    }

    public GridImpl getGrid() {
        return grid;
    }

    public LocalGridClientImpl getGridClient() {
        return gridClient;
    }

    public SymmetricSecurityConfiguration getSecurity() {
        return security;
    }

    /**
     * @return the URL the agents have to register to. Always a loopback URL: the agents run on this machine and
     * nothing outside of it has any business reaching this grid.
     */
    public String getGridUrl() {
        return "http://" + AgentConfWriter.LOOPBACK_HOST + ":" + grid.getServerPort();
    }

    /**
     * Stops the grid and deletes the files its file manager cached. The grid client is left alone: it is registered
     * in the execution engine context, which closes it itself.
     */
    @Override
    public void close() throws IOException {
        logger.debug("Stopping the local grid...");
        try {
            grid.stop();
        } catch (Exception e) {
            throw new IOException("Error while stopping the local grid", e);
        } finally {
            deleteFileManagerDirectory();
        }
    }

    /**
     * Deleted only after the grid has been stopped, which is what closes the files it was still holding. A failure is
     * not worth failing an execution which is over: the directory is swept the next time a local execution starts.
     */
    private void deleteFileManagerDirectory() {
        try {
            FileUtils.deleteDirectory(fileManagerDirectory.toFile());
        } catch (IOException e) {
            logger.warn("Failed to delete the file manager directory {} of the local grid.", fileManagerDirectory, e);
        }
    }
}
