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
package step.plugins.jmeter;

import ch.exense.commons.app.Configuration;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.FunctionPlugin;
import step.functions.type.FunctionTypeRegistry;

@Plugin(dependencies= {FunctionPlugin.class})
public class JMeterFunctionTypeLocalPlugin extends AbstractExecutionEnginePlugin {
    private FunctionTypeRegistry functionTypeRegistry;

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
        if (context.getOperationMode() == OperationMode.LOCAL) {
            Configuration config = context.getConfiguration();

            String jMeterHome = System.getenv().get("JMETER_HOME");
            if (jMeterHome != null) {
                config.putProperty("plugins.jmeter.home", jMeterHome);
            }

            functionTypeRegistry = context.require(FunctionTypeRegistry.class);
            functionTypeRegistry.registerFunctionType(new JMeterFunctionType(context.getConfiguration()));
        }
    }

}
