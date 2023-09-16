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

import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.plugins.Plugin;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.plugins.jmeter.JMeterFunctionType;

@Plugin(dependencies = {LocalFunctionPlugin.class})
public class JmeterFunctionTypeTestPlugin extends AbstractExecutionEnginePlugin {
    private FunctionAccessor functionAccessor;
    private FunctionTypeRegistry functionTypeRegistry;

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
        // TODO: temporary solution for test
        if (context.getOperationMode() == OperationMode.LOCAL) {
            functionAccessor = context.require(FunctionAccessor.class);
            functionTypeRegistry = context.require(FunctionTypeRegistry.class);

            functionTypeRegistry.registerFunctionType(new JMeterFunctionType(context.getConfiguration()));
        }
    }

}
