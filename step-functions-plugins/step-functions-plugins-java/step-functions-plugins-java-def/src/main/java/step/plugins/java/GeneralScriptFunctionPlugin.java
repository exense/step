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

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Constants;
import step.core.agents.provisioning.driver.AgentProvisioningDriver;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.FunctionPlugin;
import step.functions.type.FunctionTypeRegistry;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;


@Plugin(dependencies = {FunctionPlugin.class})
public class GeneralScriptFunctionPlugin extends AbstractExecutionEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(GeneralScriptFunctionPlugin.class);

    private FunctionTypeRegistry functionTypeRegistry;

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
        if (context.getOperationMode().isLocal()) {
            functionTypeRegistry = context.require(FunctionTypeRegistry.class);
            functionTypeRegistry.registerFunctionType(new GeneralScriptFunctionType(context.getConfiguration()));
        }

        // The driver of the agents: on a controller, including the IDE, it is in the parent context. In the CLI,
        // LocalAgentProvisioningPlugin publishes it in this context, as it runs before FunctionPlugin, which this plugin depends on.
        AgentProvisioningDriver driver = parentContext != null ? parentContext.get(AgentProvisioningDriver.class) : null;
        if (driver == null) {
            driver = context.get(AgentProvisioningDriver.class);
        }
        if (driver != null) {
            declareScriptEngineLibraries(context.getConfiguration(), driver);
        }
    }

    /**
     * Points {@code plugins.<language>.libs} at the script engine libraries when the agents are started with an auto-provisioning
     * requiring libraries extraction.
     * A value already configured in properties still wins, so that an agent can be sent a different Groovy than the one this application
     * runs on.
     */
    static void declareScriptEngineLibraries(Configuration configuration, AgentProvisioningDriver driver) {
        Objects.requireNonNull(configuration, "configuration must not be null");
        Objects.requireNonNull(driver, "driver must not be null");
        Path librariesDirectory = driver.getLocalLibrariesDirectory();
        if (librariesDirectory == null) {
            // The agents of this driver do not run from this application
            return;
        }
        for (ScriptEngineLibraries.ScriptEngine engine : List.of(ScriptEngineLibraries.GROOVY, ScriptEngineLibraries.JAVASCRIPT)) {
            String property = "plugins." + engine.language() + ".libs";
            if (configuration.getProperty(property, null) != null) {
                continue;
            }
            // Per version, so that an upgrade of the application does not reuse the libraries of the previous one
            Path directory = librariesDirectory.resolve(engine.language()).resolve(Constants.STEP_VERSION_STRING);
            try {
                Path libraries = ScriptEngineLibraries.resolve(engine, directory);
                if (libraries != null) {
                    configuration.putProperty(property, libraries.toString());
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
