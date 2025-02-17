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
package step.engine.plugins;

import step.core.accessors.AbstractAccessor;
import step.core.accessors.Accessor;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.encryption.EncryptionManager;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.plugins.Plugin;
import step.parameter.Parameter;
import step.parameter.ParameterManager;
import step.plugins.parametermanager.ParameterManagerPlugin;

@Plugin(dependencies= {BasePlugin.class})
public class ParameterManagerLocalPlugin extends ParameterManagerPlugin {

    public static final String STEP_PARAMTER_SCRIPT_ENGINE = "StepParamterScriptEngine";
    public static final String defaultScriptEngine = "groovy";

    public ParameterManagerLocalPlugin() {
        super();
    }

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext executionEngineContext) {
        if (executionEngineContext.getOperationMode() != OperationMode.LOCAL) {
            return;
        }

        // for AutomationPackageLocalOSPlugin we need to register ParameterManager in execution engine
        super.initializeExecutionEngineContext(parentContext, executionEngineContext);

        Accessor<Parameter> parameterAccessor = new AbstractAccessor<>(new InMemoryCollection<>());
        EncryptionManager encryptionManager = executionEngineContext.get(EncryptionManager.class);

        String scriptEngine = getScriptEngine();
        ParameterManager parameterManager = new ParameterManager(parameterAccessor, encryptionManager, scriptEngine, executionEngineContext.getDynamicBeanResolver());
        executionEngineContext.put(ParameterManager.class, parameterManager);

        configure(parameterManager);
    }

    private String getScriptEngine() {
        String scriptEngine = defaultScriptEngine;
        String propertyVar = System.getProperty(STEP_PARAMTER_SCRIPT_ENGINE);
        if (propertyVar != null && !propertyVar.isBlank()) {
            scriptEngine = propertyVar;
        } else {
            String envVar = System.getenv(STEP_PARAMTER_SCRIPT_ENGINE);
            if (envVar != null && ! envVar.isBlank()) {
                scriptEngine = envVar;
            }
        }
        return scriptEngine;
    }

}
