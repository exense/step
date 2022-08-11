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

import ch.exense.commons.app.Configuration;
import step.artefacts.reports.CheckReportNode;
import step.artefacts.reports.EchoReportNode;
import step.artefacts.reports.RetryIfFailsReportNode;
import step.artefacts.reports.SleepReportNode;
import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.execution.LeafReportNodesFilter;
import step.core.execution.LeafReportNodesTableParameters;
import step.framework.server.tables.service.TableParameters;

import java.util.ArrayList;
import java.util.List;

public class LeafReportNodeTable extends ReportNodeTable {
	
	private final List<String[]> optionalReportNodesFilter;
	
	public LeafReportNodeTable(GlobalContext context, Collection<ReportNode> collection) {
		super(context, collection);
		
		Configuration configuration = context.getConfiguration();
		String optionalReportNodesFilterStr = configuration.getProperty("execution.reports.nodes.include", 
				"_class:"+EchoReportNode.class.getName()+","+
				"_class:"+RetryIfFailsReportNode.class.getName()+","+
				"_class:"+SleepReportNode.class.getName()+","+
				"_class:"+CheckReportNode.class.getName()+","+
				"_class:step.artefacts.reports.WaitForEventReportNode");
		optionalReportNodesFilter = new ArrayList<String[]>();
		for (String kv: optionalReportNodesFilterStr.split(",")) {
			optionalReportNodesFilter.add(kv.split(":"));
		}
	}

	@Override
	public List<Filter> getTableFilters(TableParameters queryParameters) {
		List<Filter> filters = super.getTableFilters(queryParameters);
		if(filters == null) {
			filters = new ArrayList<>();
		}
		filters.add(new LeafReportNodesFilter(optionalReportNodesFilter).buildAdditionalQuery((LeafReportNodesTableParameters) queryParameters));
		return filters;
	}

}
