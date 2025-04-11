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
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.collections.Collection;
import step.core.collections.SearchOrder;
import step.core.execution.model.ExecutionStatus;
import step.core.execution.table.*;
import step.core.execution.type.ExecutionTypeControllerPlugin;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.plugins.screentemplating.ScreenTemplatePlugin;

import java.util.ArrayList;
import java.util.List;

@Plugin(dependencies= {ExecutionTypeControllerPlugin.class, ScreenTemplatePlugin.class})
public class ExecutionPlugin extends AbstractControllerPlugin {

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		TableRegistry tableRegistry = context.get(TableRegistry.class);
		
		Collection<ExecutionWrapper> collection = context.getCollectionFactory().getCollection("executions",
				ExecutionWrapper.class);
		Collection<ReportNode> reportsCollection = context.getCollectionFactory().getCollection("reports",
				ReportNode.class);

		RootReportNodeProvider rootReportNodeFormatter = new RootReportNodeProvider(context);
		ExecutionSummaryProvider executionSummaryFormatter = new ExecutionSummaryProvider(context);
		tableRegistry.register("executions", new Table<>(collection, "execution-read", true).withResultItemEnricher(execution->{
			execution.setRootReportNode(rootReportNodeFormatter.getRootReportNode(execution));
			Object executionSummary = executionSummaryFormatter.format(execution);
			execution.setExecutionSummary(executionSummary);
			ExecutionStatus status = execution.getStatus();
			ReportNodeStatus result = execution.getResult();
			if (status != null) {
				execution.setEffectiveStatus(status.equals(ExecutionStatus.ENDED) ? ((result != null) ? result.name() : status.name()) : status.name());
			} else if (result != null) {
				execution.setEffectiveStatus(result.name());
			}
			return execution;
		}).withDerivedTableSortingFactory(sort -> {
			if (sort.getField().equals("effectiveStatus")) {
				int orderValue = sort.getDirection().getValue();
				return new SearchOrder(List.of(new SearchOrder.FieldSearchOrder("status", orderValue),
						new SearchOrder.FieldSearchOrder("result", orderValue)));
			} else {
				return null;
			}

		}));


		tableRegistry.register("leafReports", new Table<>(reportsCollection, "execution-read", false)
				.withTableFiltersFactory(new LeafReportNodeTableFilterFactory(context)).withResultListFactory(()->new ArrayList<>(){}));
		tableRegistry.register("reports", new Table<>(reportsCollection, "execution-read", false)
				.withTableFiltersFactory(new ReportNodeTableFilterFactory()).withResultListFactory(()->new ArrayList<>(){}));
		context.getServiceRegistrationCallback().registerService(ExecutionServices.class);
	}
}
