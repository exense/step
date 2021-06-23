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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.common.managedoperations.Operation;
import step.core.GlobalContext;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.artefacts.reports.ReportNode;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.execution.threadmanager.ThreadManager;
import step.core.execution.type.TestcaseReportNodesFilter;
import step.core.tables.TableColumn;
import step.core.tables.formatters.DateFormatter;

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestcaseReportNodeTable extends ReportNodeTable {

	private static final Logger logger = LoggerFactory.getLogger(TestcaseReportNodeTable.class);
	
	private ThreadManager threadManager;
	
	public TestcaseReportNodeTable(GlobalContext context, Collection<ReportNode> collection) {
		super(context, collection);
		threadManager = context.get(ThreadManager.class);
	}

	@Override
	public List<Filter> getAdditionalQueryFragments(JsonObject queryParameters) {
		List<Filter> additionalQueryFragments = super.getAdditionalQueryFragments(queryParameters);
		if(additionalQueryFragments == null) {
			additionalQueryFragments = new ArrayList<>();
		}
		additionalQueryFragments.add(new TestcaseReportNodesFilter().buildAdditionalQuery(queryParameters));
		return additionalQueryFragments;
	}

	@Override
	public ReportNode enrichEntity(ReportNode entity) {
		List<Operation> currentOperationsByReportNodeId = threadManager.getCurrentOperationsByReportNodeId(entity.getId().toString());
		String operations = "[]";
		try {
			operations= DefaultJacksonMapperProvider.getObjectMapper().writeValueAsString(currentOperationsByReportNodeId);
		} catch (JsonProcessingException e) {
			logger.error("Unable to convert list of current operations to json string.",e);
		}
		entity.addCustomAttribute("operations",operations);
		return entity;
	}

	public Map<String, TableColumn> getExportFields() {
		Map<String, TableColumn> result = new LinkedHashMap<String,TableColumn> ();
		result.put("executionTime",new TableColumn("executionTime","Begin",new DateFormatter("dd.MM.yyyy HH:mm:ss")));
		result.put("name",new TableColumn("name","Name"));
		result.put("functionAttributes",new TableColumn("functionAttributes","Keyword"));
		result.put("status",new TableColumn("status","Status"));
		result.put("error",new TableColumn("error","Error"));
		result.put("input",new TableColumn("input","Input"));
		result.put("output",new TableColumn("output","Output"));
		result.put("duration",new TableColumn("duration","Duration"));
		result.put("adapter",new TableColumn("agentUrl","Agent"));
		return result;
	}

}
