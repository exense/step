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


import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;
import javax.json.JsonString;

import ch.exense.commons.core.collections.Filter;
import ch.exense.commons.core.collections.Filters;
import step.core.tables.TableQueryFactory;

public class LeafReportNodesFilter implements TableQueryFactory {
	
	protected List<String[]> optionalReportNodesFilter = new ArrayList<String[]>() ;
	
	public LeafReportNodesFilter(List<String[]> optionalReportNodesFilter) {
		super();
		this.optionalReportNodesFilter = optionalReportNodesFilter;
	}

	public Filter buildAdditionalQuery(JsonObject filter) {		
		List<Filter> fragments = new ArrayList<>();
		if(filter != null && filter.containsKey("eid")) {
			fragments.add(Filters.equals("executionID", filter.getString("eid")));
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
		if(filter != null && filter.containsKey("testcases")) {
			//customAttributes.TestCase
			List<String> testcaseIds = new ArrayList<>();
			filter.getJsonArray("testcases").forEach(v->testcaseIds.add(((JsonString)v).getString()));
			fragments.add(Filters.in("customAttributes.TestCase", testcaseIds));
		}
		
		return Filters.and(fragments);
	}
}
