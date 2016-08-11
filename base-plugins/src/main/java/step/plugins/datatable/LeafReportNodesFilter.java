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
