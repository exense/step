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


import step.core.collections.Filter;
import step.core.collections.Filters;
import step.framework.server.tables.service.TableParameters;

import java.util.ArrayList;
import java.util.List;

public class LeafReportNodesFilter {
	
	protected List<String[]> optionalReportNodesFilter;
	
	public LeafReportNodesFilter(List<String[]> optionalReportNodesFilter) {
		super();
		this.optionalReportNodesFilter = optionalReportNodesFilter;
	}

	public Filter buildAdditionalQuery(LeafReportNodesTableParameters parameters) {
		List<Filter> fragments = new ArrayList<>();
		if(parameters != null) {
			String eid = parameters.getEid();
			if(eid != null) {
				fragments.add(Filters.equals("executionID", eid));
			}
		}
		
		List<Filter> nodeFilters = new ArrayList<>();
		nodeFilters.add(Filters.equals("_class","step.artefacts.reports.CallFunctionReportNode"));
		nodeFilters.add(Filters.equals("error.root",true));
		if(optionalReportNodesFilter != null) {
			for (String[] kv: optionalReportNodesFilter) {
				nodeFilters.add(Filters.equals(kv[0], kv[1]));	
			}
		}
		fragments.add(Filters.or(nodeFilters));
		if(parameters != null) {
			List<String> testcases = parameters.getTestcases();
			if(testcases != null) {
				fragments.add(Filters.in("customAttributes.TestCase", testcases));
			}
		}
		
		return Filters.and(fragments);
	}


}
