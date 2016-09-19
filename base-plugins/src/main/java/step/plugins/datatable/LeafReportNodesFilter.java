/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.plugins.datatable;

import javax.json.JsonObject;

public class LeafReportNodesFilter implements CollectionQueryFactory {

	public String buildAdditionalQuery(JsonObject filter) {
		StringBuilder query = new StringBuilder();
		query.append("executionID: '" + filter.getString("eid") + "', $or: [ { _class: 'step.artefacts.reports.CallFunctionReportNode' }, { status: 'TECHNICAL_ERROR'} ]");
		if(filter.containsKey("testcases")) {
			query.append(", customAttributes.TestCase: { $in: "+filter.getJsonArray("testcases").toString()+"}");
		}
		return query.toString();
		
//		filter.getJsonArray(name)
		
//		StringBuffer populatedQuery = new StringBuffer(); 
//		String query = table.getQuery();
//		if(query!=null) {
//			Pattern p = Pattern.compile("\\{(.+?)\\}");
//			Matcher m = p.matcher(query);
//			while(m.find()) {
//				String key = m.group(1);
//				String value = params.getFirst(key);
//				if(value!=null) {
//					m.appendReplacement(populatedQuery, "'"+value+"'");
//				}
//			}
//			m.appendTail(populatedQuery);
//			return populatedQuery.toString();
//		} else {
//			return null;
//		}
//		return null;
	}
}
