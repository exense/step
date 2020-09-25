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
package step.core.execution;

import step.core.GlobalContext;
import step.core.accessors.collections.CollectionRegistry;
import step.core.execution.table.ExecutionCollection;
import step.core.execution.table.LeafReportNodeCollection;
import step.core.execution.table.ReportNodeCollection;
import step.core.execution.type.ExecutionTypePlugin;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.plugins.screentemplating.ScreenTemplatePlugin;

@Plugin(dependencies= {ExecutionTypePlugin.class, ScreenTemplatePlugin.class})
public class ExecutionPlugin extends AbstractControllerPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		CollectionRegistry collectionRegistry = context.get(CollectionRegistry.class);
		collectionRegistry.register("executions", new ExecutionCollection(context));
		collectionRegistry.register("leafReports", new LeafReportNodeCollection(context));
		collectionRegistry.register("reports", new ReportNodeCollection(context));
		context.getServiceRegistrationCallback().registerService(ExecutionServices.class);
	}
}
