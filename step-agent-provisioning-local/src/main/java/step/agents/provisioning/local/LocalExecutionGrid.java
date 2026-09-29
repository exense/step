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
import java.util.Objects;

/**
 * The grid the local agents register to.
 * <p>
 * It is the very same {@link GridImpl} a Step controller runs. It is either embedded, started on an ephemeral port
 * for the lifetime of one CLI invocation (see {@link #startEmbedded}), or the grid of the application the local
 * agents are started from, typically the one of the Step IDE (see {@link #attach}). Reusing it rather than emulating
 * it is what makes a local execution go through the same path as a platform execution: the same token selection, the
 * same file transfer to the agents, the same keyword protocol.
 */
public class LocalExecutionGrid implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(LocalExecutionGrid.class);
    private static final int SECRET_KEY_LENGTH_BYTES = 32;

    private final GridImpl grid;
    private final LocalGridClientImpl gridClient;
    private final SymmetricSecurityConfiguration security;
    /**
     * The file manager directory of the embedded grid, null when attached to a grid this instance doesn't own
     */
    private final Path fileManagerDirectory;

    protected LocalExecutionGrid(GridImpl grid, SymmetricSecurityConfiguration security, Path fileManagerDirectory,
                                 Duration agentStartTimeout) {
        this.grid = grid;
        this.security = security;
        this.fileManagerDirectory = fileManagerDirectory;
        this.gridClient = new LocalGridClientImpl(gridClientConfiguration(agentStartTimeout, security), grid);
    }

    /**
     * Starts an embedded grid, stopped by {@link #close()}.
     *
     * @param workspace the workspace the file manager of this grid caches its files in. Using it rather than a
     *                  temporary directory of its own is what gets that cache deleted: it is deleted with this grid,
     *                  and swept by the next run should this CLI be killed before it can do so.
     */
    public static LocalExecutionGrid startEmbedded(Duration agentStartTimeout, LocalAgentWorkspace workspace) throws Exception {
        Objects.requireNonNull(agentStartTimeout, "agentStartTimeout must not be null");
        Objects.requireNonNull(workspace, "workspace must not be null");
        // The grid listens on all interfaces, so it is protected with a secret rather than left open to anything
        // able to connect. The secret is generated per invocation and never leaves this process and the
        // configuration files of the agents it starts, both of which are gone when the CLI terminates.
        SymmetricSecurityConfiguration security = new SymmetricSecurityConfiguration(generateSecretKey());

        GridImpl.GridImplConfig gridConfig = new GridImpl.GridImplConfig();
        gridConfig.setSecurity(security);

        Path fileManagerDirectory = workspace.createGridRunDirectory();

        // Port 0: the OS assigns a free port, which keeps concurrent CLI invocations from colliding
        GridImpl grid = new GridImpl(fileManagerDirectory.toFile(), 0, gridConfig);
        grid.start();
        logger.debug("Started the local grid on port {}", grid.getServerPort());

        return new LocalExecutionGrid(grid, security, fileManagerDirectory, agentStartTimeout);
    }

    /**
     * Attaches to a grid which is already running and owned by someone else, typically the grid of the Step IDE.
     * {@link #close()} leaves that grid running.
     *
     * @param security the security configuration of that grid, passed on to the agents for them to be able to
     *                 register to it. May be null if the grid is not secured.
     */
    public static LocalExecutionGrid attach(GridImpl grid, SymmetricSecurityConfiguration security, Duration agentStartTimeout) {
        Objects.requireNonNull(grid, "grid must not be null");
        Objects.requireNonNull(agentStartTimeout, "agentStartTimeout must not be null");
        logger.debug("Attached to the grid running on port {}", grid.getServerPort());
        return new LocalExecutionGrid(grid, security, null, agentStartTimeout);
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
     * Closes the grid client of this instance and, for an embedded grid, stops the grid and deletes the files its file
     * manager cached. An attached grid is left running.
     * <p>
     * The grid client is closed first, as the class loaders of its local tokens read files that are deleted with the
     * file manager directory. Closing it is idempotent: the CLI also registers it in the execution engine context,
     * which closes it when the engine is closed.
     */
    @Override
    public void close() throws IOException {
        gridClient.close();
        if (fileManagerDirectory == null) {
            return;
        }
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
