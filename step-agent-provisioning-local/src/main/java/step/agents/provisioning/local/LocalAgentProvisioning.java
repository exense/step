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

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Builds the pieces of the local agent provisioning shared by every application offering it: the CLI, which runs its
 * own grid, and the Step IDE, which attaches to the grid it already runs.
 */
public final class LocalAgentProvisioning {

    private static final Logger logger = LoggerFactory.getLogger(LocalAgentProvisioning.class);

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

    /**
     * Points {@code plugins.<language>.libs} at the script engine libraries, the way the step.properties of a
     * controller does. Without them a Groovy or JavaScript keyword reaches the agent and fails there with "Unable to
     * find script engine": the engine lives in this application, and the agent runs in its own process with its own
     * class path.
     * <p>
     * A value already configured wins, so that an agent can be sent a different Groovy than the one this application
     * runs on.
     */
    public static void declareScriptEngineLibraries(Configuration configuration, LocalAgentWorkspace workspace) {
        Objects.requireNonNull(configuration, "configuration must not be null");
        Objects.requireNonNull(workspace, "workspace must not be null");
        ScriptEngineLibraries libraries = new ScriptEngineLibraries(workspace);
        for (ScriptEngineLibraries.ScriptEngine engine : List.of(ScriptEngineLibraries.GROOVY, ScriptEngineLibraries.JAVASCRIPT)) {
            String property = "plugins." + engine.language() + ".libs";
            if (configuration.getProperty(property, null) != null) {
                continue;
            }
            try {
                Path directory = libraries.resolve(engine);
                if (directory != null) {
                    configuration.putProperty(property, directory.toString());
                }
            } catch (Exception e) {
                // Not worth aborting the execution: only the keywords of that language are affected, and they fail
                // with an error of their own naming the missing engine. Every exception is caught, not only the
                // expected one: resolving the engines reads how the application itself is packaged, and the way that
                // fails is not ours to predict.
                logger.warn("The {} keywords will not be executable: unable to provide the script engine to the agents.",
                    engine.language(), e);
            }
        }
    }
}
