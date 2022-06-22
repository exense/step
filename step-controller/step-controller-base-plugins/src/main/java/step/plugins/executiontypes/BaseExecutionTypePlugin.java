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
package step.plugins.executiontypes;

import step.core.GlobalContext;
import step.core.execution.type.ExecutionTypeManager;
import step.core.execution.type.ExecutionTypePlugin;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.plugins.views.ViewControllerPlugin;

@Plugin(dependencies= {ExecutionTypePlugin.class, ViewControllerPlugin.class})
public class BaseExecutionTypePlugin extends AbstractControllerPlugin {

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		super.serverStart(context);
		
		ExecutionTypeManager executionTypeManager = context.get(ExecutionTypeManager.class);
		executionTypeManager.put(new DefaultExecutionType(context));
		executionTypeManager.put(new TestSetExecutionType(context));

	}

}
