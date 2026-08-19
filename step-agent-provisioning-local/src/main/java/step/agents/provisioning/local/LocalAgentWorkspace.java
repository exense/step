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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The directory the local agents are installed and run in.
 * <p>
 * It holds things with very different lifetimes:
 * <ul>
 *   <li>{@code agents/} and {@code libraries/} — the agents themselves, extracted from the CLI or installed with npm
 *       on first use, and the libraries sent to them. <b>Kept</b> across runs: re-extracting 20 MB or re-running an
 *       npm install on every execution would make the local execution needlessly slow.</li>
 *   <li>{@code agent-*} — one throw-away directory per running agent, deleted when it is stopped.</li>
 *   <li>{@code grid-*} — the file manager of the embedded grid, deleted when the grid is stopped.</li>
 * </ul>
 * A CLI killed with a SIGKILL, or a developer closing the terminal, never gets the chance to clean up, so the
 * throw-away directories left over by a previous run are swept when the workspace is created rather than being left
 * to accumulate. This mirrors what {@code AgentForker.cleanupStaleForkedAgentFiles} does for forked agents. The
 * installed agents and libraries are of course not swept.
 */
public class LocalAgentWorkspace {

    private static final Logger logger = LoggerFactory.getLogger(LocalAgentWorkspace.class);
    private static final String WORKSPACE_DIRECTORY_NAME = "step-cli-local-agents";
    private static final String AGENT_DIRECTORY_PREFIX = "agent-";
    private static final String GRID_DIRECTORY_PREFIX = "grid-";
    private static final String INSTALLED_AGENTS_DIRECTORY_NAME = "agents";
    private static final String INSTALLED_LIBRARIES_DIRECTORY_NAME = "libraries";

    private final Path root;

    public LocalAgentWorkspace(Path configuredRoot) throws IOException {
        this.root = configuredRoot != null ? configuredRoot.toAbsolutePath() : defaultRoot();
        Files.createDirectories(root);
        logger.debug("Using local agent workspace {}", root);
        sweepStaleRunDirectories();
    }

    private static Path defaultRoot() {
        return Path.of(System.getProperty("java.io.tmpdir")).resolve(WORKSPACE_DIRECTORY_NAME).toAbsolutePath();
    }

    /**
     * Removes the throw-away directories left behind by a previous run. Any directory found here at this point
     * belongs to a run which is over: the CLI is a short lived process which cleans its own directories up as it
     * stops the agents and the grid they belong to.
     */
    private void sweepStaleRunDirectories() {
        File[] staleDirectories = root.toFile().listFiles((dir, name) ->
            (name.startsWith(AGENT_DIRECTORY_PREFIX) || name.startsWith(GRID_DIRECTORY_PREFIX))
                && new File(dir, name).isDirectory());
        if (staleDirectories == null) {
            return;
        }
        for (File staleDirectory : staleDirectories) {
            logger.info("Removing the directory {} left over by a previous run.", staleDirectory);
            try {
                FileUtils.deleteDirectory(staleDirectory);
            } catch (IOException e) {
                logger.warn("Failed to delete the stale directory {}.", staleDirectory, e);
            }
        }
    }

    /**
     * Creates a dedicated, empty directory for one running agent.
     *
     * @param agentType the agent type, to make the directory recognizable while the CLI runs
     */
    public Path createAgentRunDirectory(String agentType) throws IOException {
        return Files.createTempDirectory(root, AGENT_DIRECTORY_PREFIX + agentType + "-");
    }

    /**
     * Creates a dedicated, empty directory for the file manager of the embedded grid, which caches there everything
     * the grid sends to the agents.
     */
    public Path createGridRunDirectory() throws IOException {
        return Files.createTempDirectory(root, GRID_DIRECTORY_PREFIX);
    }

    /**
     * The directory an agent is installed in, kept across runs.
     *
     * @param agentName a name identifying the agent, e.g. {@code java}
     * @param version   the version of the agent. Part of the path so that a CLI upgrade installs its own agent next
     *                  to the previous one instead of silently running a stale one.
     * @return the directory, which is <b>not</b> created by this method: its existence is what tells the provider
     * the agent is already installed.
     */
    public Path getInstalledAgentDirectory(String agentName, String version) {
        return root.resolve(INSTALLED_AGENTS_DIRECTORY_NAME).resolve(agentName).resolve(version);
    }

    /**
     * The directory a set of libraries sent to the agents is installed in, kept across runs for the same reason as
     * the agents themselves.
     *
     * @param name    a name identifying the library set, e.g. {@code groovy}
     * @param version the version they were extracted from, so that a CLI upgrade does not reuse stale libraries
     * @return the directory, which is <b>not</b> created by this method
     */
    public Path getInstalledLibrariesDirectory(String name, String version) {
        return root.resolve(INSTALLED_LIBRARIES_DIRECTORY_NAME).resolve(name).resolve(version);
    }
}
