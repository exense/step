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

import java.util.List;
import java.util.Objects;

/**
 * Builds the pieces of the local agent provisioning shared by every application offering it: the CLI, which runs its
 * own grid, and the Step IDE, which attaches to the grid it already runs.
 */
public final class LocalAgentProvisioning {

    private LocalAgentProvisioning() {
    }

    /**
     * @return a driver starting the Java, Node.js and .NET agents of this distribution, registering them to the given grid
     */
    public static LocalProcessAgentProvisioningDriver createDriver(LocalExecutionGrid grid, LocalAgentWorkspace workspace,
                                                                   LocalAgentProvisioningConfiguration configuration) {
        Objects.requireNonNull(grid, "grid must not be null");
        Objects.requireNonNull(workspace, "workspace must not be null");
        Objects.requireNonNull(configuration, "configuration must not be null");
        return new LocalProcessAgentProvisioningDriver(grid, workspace, configuration, List.of(
            new JavaLocalAgentProvider(configuration, workspace),
            new NodeLocalAgentProvider(configuration, workspace),
            new DotNetLocalAgentProvider(configuration)));
    }
}
