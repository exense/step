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
import step.core.artefacts.reports.ReportNode;
import step.core.collections.Collection;
import step.core.execution.table.ExecutionTable;
import step.core.execution.table.ExecutionWrapper;
import step.core.execution.table.LeafReportNodeTable;
import step.core.execution.table.ReportNodeTable;
import step.core.execution.type.ExecutionTypePlugin;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.tables.TableRegistry;
import step.plugins.screentemplating.ScreenTemplatePlugin;

@Plugin(dependencies= {ExecutionTypePlugin.class, ScreenTemplatePlugin.class})
public class ExecutionPlugin extends AbstractControllerPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		TableRegistry tableRegistry = context.get(TableRegistry.class);
		
		Collection<ExecutionWrapper> collection = context.getCollectionFactory().getCollection("executions",
				ExecutionWrapper.class);
		Collection<ReportNode> reportsCollection = context.getCollectionFactory().getCollection("reports",
				ReportNode.class);

		tableRegistry.register("executions", new ExecutionTable(context, collection));
		tableRegistry.register("leafReports", new LeafReportNodeTable(context, reportsCollection));
		tableRegistry.register("reports", new ReportNodeTable(context, reportsCollection));
		context.getServiceRegistrationCallback().registerService(ExecutionServices.class);
	}
}
