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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.JsonObject;

import org.bson.conversions.Bson;

import ch.exense.commons.app.Configuration;
import step.artefacts.reports.CheckReportNode;
import step.artefacts.reports.EchoReportNode;
import step.artefacts.reports.RetryIfFailsReportNode;
import step.artefacts.reports.SleepReportNode;
import step.core.GlobalContext;
import step.core.accessors.collections.field.CollectionField;
import step.core.accessors.collections.field.formatter.DateFormatter;
import step.core.execution.LeafReportNodesFilter;

public class LeafReportNodeCollection extends ReportNodeCollection {
	
	private final List<String[]> optionalReportNodesFilter;
	
	public LeafReportNodeCollection(GlobalContext context) {
		super(context);
		
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
	public List<Bson> getAdditionalQueryFragments(JsonObject queryParameters) {
		List<Bson> additionalQueryFragments = super.getAdditionalQueryFragments(queryParameters);
		if(additionalQueryFragments == null) {
			additionalQueryFragments = new ArrayList<>();
		}
		additionalQueryFragments.add(new LeafReportNodesFilter(optionalReportNodesFilter).buildAdditionalQuery(queryParameters));
		return additionalQueryFragments;
	}
	
	@Override
	protected Map<String, CollectionField> getExportFields() {
		Map<String, CollectionField> result = new LinkedHashMap<String,CollectionField> ();
		result.put("executionTime",new CollectionField("executionTime","Begin",new DateFormatter("dd.MM.yyyy HH:mm:ss")));
		result.put("name",new CollectionField("name","Name"));
		result.put("functionAttributes",new CollectionField("functionAttributes","Keyword"));
		result.put("status",new CollectionField("status","Status"));
		result.put("error",new CollectionField("error","Error"));
		result.put("input",new CollectionField("input","Input"));
		result.put("output",new CollectionField("output","Output"));
		result.put("duration",new CollectionField("duration","Duration"));
		result.put("adapter",new CollectionField("adapter","Adapter"));
		return result;
	}

}
