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

import step.grid.security.SymmetricSecurityConfiguration;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Everything a {@link LocalAgentProvider} needs to start one local agent process.
 */
public class LocalAgentStartContext {

    private final String gridUrl;
    private final Path workingDirectory;
    private final int numberOfTokens;
    private final Map<String, String> tokenAttributes;
    private final SymmetricSecurityConfiguration gridSecurity;

    /**
     * @param gridUrl          the URL of the embedded grid the agent has to register to. Always a loopback URL.
     * @param workingDirectory the directory the agent process runs in. It is created by the caller, is dedicated to
     *                         this agent and is deleted once the agent is stopped.
     * @param numberOfTokens   the capacity of the single token group the agent has to declare
     * @param tokenAttributes  the attributes to declare on the tokens. Contains at least the token partition, which
     *                         isolates the tokens of this execution from the ones of any other.
     * @param gridSecurity     the security configuration of the embedded grid, or {@code null} when the grid
     *                         authentication is disabled
     */
    public LocalAgentStartContext(String gridUrl, Path workingDirectory, int numberOfTokens,
                                  Map<String, String> tokenAttributes, SymmetricSecurityConfiguration gridSecurity) {
        this.gridUrl = Objects.requireNonNull(gridUrl, "gridUrl must not be null");
        this.workingDirectory = Objects.requireNonNull(workingDirectory, "workingDirectory must not be null");
        this.numberOfTokens = numberOfTokens;
        this.tokenAttributes = Objects.requireNonNull(tokenAttributes, "tokenAttributes must not be null");
        this.gridSecurity = gridSecurity;
    }

    public String getGridUrl() {
        return gridUrl;
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public int getNumberOfTokens() {
        return numberOfTokens;
    }

    public Map<String, String> getTokenAttributes() {
        return tokenAttributes;
    }

    public SymmetricSecurityConfiguration getGridSecurity() {
        return gridSecurity;
    }
}
