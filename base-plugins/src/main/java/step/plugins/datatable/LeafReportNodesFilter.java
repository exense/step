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

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.or;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;
import javax.json.JsonString;

import org.bson.Document;
import org.bson.conversions.Bson;

public class LeafReportNodesFilter implements CollectionQueryFactory {

	public Bson buildAdditionalQuery(JsonObject filter) {		
		List<Bson> fragments = new ArrayList<>();
		if(filter.containsKey("eid")) {
			fragments.add(new Document("executionID", filter.getString("eid")));
		}
		
		fragments.add(or(new Document("_class","step.artefacts.reports.CallFunctionReportNode"),new Document("status","TECHNICAL_ERROR")));
		if(filter.containsKey("testcases")) {
			//customAttributes.TestCase
			List<String> testcaseIds = new ArrayList<>();
			filter.getJsonArray("testcases").forEach(v->testcaseIds.add(((JsonString)v).getString()));
			fragments.add(in("customAttributes.TestCase",testcaseIds));
		}
		
		return and(fragments);
	}
}
