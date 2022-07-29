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
package step.core.execution.table;

import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.collections.Collection;
import step.core.execution.model.ExecutionStatus;
import step.framework.server.tables.AbstractTable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExecutionTable extends AbstractTable<ExecutionWrapper> {
	
	private final ExecutionSummaryProvider executionSummaryFormatter;
	private final RootReportNodeProvider rootReportNodeFormatter;

	public ExecutionTable(GlobalContext context, Collection<ExecutionWrapper> collection) {
		super(collection, "execution-read", true);
		RootReportNodeProvider rootReportNodeFormatter = new RootReportNodeProvider(context);
		ExecutionSummaryProvider executionSummaryFormatter = new ExecutionSummaryProvider(context);
		this.executionSummaryFormatter = executionSummaryFormatter;
		this.rootReportNodeFormatter = rootReportNodeFormatter;
	}

	@Override
	public ExecutionWrapper enrichEntity(ExecutionWrapper execution) {
		execution.setRootReportNode(rootReportNodeFormatter.getRootReportNode(execution));
		Object executionSummary = executionSummaryFormatter.format(execution);
		execution.setExecutionSummary(executionSummary);
		return execution;
	}
}
