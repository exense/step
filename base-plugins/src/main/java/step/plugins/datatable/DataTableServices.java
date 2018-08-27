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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import step.core.accessors.Collection;
import step.core.accessors.CollectionFind;
import step.core.accessors.SearchOrder;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.execution.model.ExecutionStatus;
import step.core.export.ExportTaskManager;
import step.core.export.ExportTaskManager.ExportRunnable;
import step.core.export.ExportTaskManager.ExportStatus;
import step.plugins.datatable.formatters.custom.ExecutionSummaryFormatter;
import step.plugins.screentemplating.Input;
import step.plugins.screentemplating.ScreenTemplatePlugin;

@Singleton
@Path("datatable")
public class DataTableServices extends AbstractServices {
	
	private static final Logger logger = LoggerFactory.getLogger(DataTableServices.class);
	
	protected DataTableRegistry dataTableRegistry;
	
	protected ExportTaskManager exportTaskManager;
	
	ExecutorService reportExecutor = Executors.newFixedThreadPool(2);
	
	@PostConstruct
	public void init() {
		exportTaskManager = new ExportTaskManager(getContext().getAttachmentManager());
		
		MongoDatabase database = getContext().getMongoClientSession().getMongoDatabase();
		
		dataTableRegistry = getContext().get(DataTableRegistry.class);
		
		BackendDataTable executions = new BackendDataTable(new Collection(database, "executions"));
		executions.addColumn("ID", "_id").addColumn("Description", "description").addDateColumn("Start time", "startTime")
		.addDateColumn("End time", "endTime").addColumn("User", "executionParameters.userID");
				
		ScreenTemplatePlugin screenTemplates = (ScreenTemplatePlugin) getContext().get(ScreenTemplatePlugin.SCREEN_TEMPLATE_KEY);
		if(screenTemplates!=null) {
			for(Input input:screenTemplates.getInputsForScreen("executionTable", null)) {
				executions.addColumn(input.getLabel(), input.getId());
			}
		}
		executions.addTextWithDropdownColumn("Status", "status", Arrays.asList(ExecutionStatus.values()).stream().map(Object::toString).collect(Collectors.toList()));
		executions.addCustomColumn("Summary", new ExecutionSummaryFormatter(controller.getContext()));
		
		ColumnBuilder leafReportNodesColumns = new ColumnBuilder();
		leafReportNodesColumns.addDateColumn("Begin", "executionTime").addColumn("Name","name").addColumn("Status","status").addColumn("Error", "error")
		.addColumn("Input","input").addColumn("Output","output").addColumn("Duration","duration").addColumn("Adapter", "adapter");

		// Report table
		
		List<String> reportSearchAttributes = new ArrayList<>();
		if(screenTemplates!=null) {
			for(Input input:screenTemplates.getInputsForScreen("functionTable", null)) {
				reportSearchAttributes.add("functionAttributes."+input.getId());
			}
		}
		reportSearchAttributes.add("input");
		reportSearchAttributes.add("output");
		reportSearchAttributes.add("error.msg");
		reportSearchAttributes.add("name");
		
		BackendDataTable leafReportNodes = new BackendDataTable(new Collection(database, "reports"));
		leafReportNodes.addColumn("ID", "_id").addTimeColumn("Begin", "executionTime").addRowAsJson("Step",reportSearchAttributes)
		.addTextWithDropdownColumnOptimized("Status", "status", Arrays.asList(ReportNodeStatus.values()).stream().map(Object::toString).collect(Collectors.toList()))
		.setQuery(new LeafReportNodesFilter()).setExportColumns(leafReportNodesColumns.build());
		
		BackendDataTable artefactTable = new BackendDataTable(new Collection(database, "artefacts"));
		artefactTable.addColumn("ID", "_id");
		if(screenTemplates!=null) {
			for(Input input:screenTemplates.getInputsForScreen("artefactTable", null)) {
				artefactTable.addColumn(input.getLabel(), input.getId());
			}
		}
		artefactTable.addColumn("Type", "_class").addRowAsJson("Actions");
		artefactTable.setQuery(new CollectionQueryFactory() {
			@Override
			public Bson buildAdditionalQuery(JsonObject filter) {
				return new Document("root", true);
			}
		});
		
		BackendDataTable functionTable = new BackendDataTable(new Collection(database, "functions"));
		functionTable.addColumn("ID", "_id");
		if(screenTemplates!=null) {
			for(Input input:screenTemplates.getInputsForScreen("functionTable", null)) {
				functionTable.addColumn(input.getLabel(), "attributes."+input.getId());
			}
		}
		functionTable.addColumn("Type", "type");
		functionTable.addRowAsJson("Actions");
		
		BackendDataTable leafReportNodesOQL = new BackendDataTable(new Collection(database, "reports"));
		leafReportNodesOQL.addColumn("ID", "_id").addColumn("Execution", "executionID").addTimeColumn("Begin", "executionTime").addRowAsJson("Step",reportSearchAttributes)
		.addArrayColumn("Attachments", "attachments").addTextWithDropdownColumn("Status", "status", Arrays.asList(ReportNodeStatus.values()).stream().map(Object::toString).collect(Collectors.toList()))
		.setQuery(new OQLFilter()).setExportColumns(leafReportNodesColumns.build());

		dataTableRegistry.addTable("executions", executions);
		dataTableRegistry.addTable("reports", leafReportNodes);
		dataTableRegistry.addTable("reportsByOQL", leafReportNodesOQL);
		dataTableRegistry.addTable("artefacts", artefactTable);
		dataTableRegistry.addTable("functions", functionTable);		

	}
	
	@PreDestroy
	public void destroy() {
		reportExecutor.shutdown();
	}
	
	Pattern columnSearchPattern = Pattern.compile("columns\\[([0-9]+)\\]\\[search\\]\\[value\\]");
	Pattern searchPattern = Pattern.compile("search\\[value\\]");
	
	@GET
	@Path("/{id}/columns")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public List<ColumnDef> getTableColumnDefs(@PathParam("id") String collectionID) {
		BackendDataTable table = dataTableRegistry.getTable(collectionID);
		return table.getColumns();
	}
	
	@POST
	@Path("/{id}/data")
	@Consumes("application/x-www-form-urlencoded")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public BackendDataTableDataResponse getTableData_Post(@PathParam("id") String collectionID, MultivaluedMap<String, String> form) throws Exception {
		return getTableData(collectionID, form);
	}
	
	@GET
	@Path("/{id}/data")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public BackendDataTableDataResponse getTableData_Get(@PathParam("id") String collectionID, @Context UriInfo uriInfo) throws Exception {
		return getTableData(collectionID, uriInfo.getQueryParameters());
	}
	
	private BackendDataTableDataResponse getTableData(@PathParam("id") String collectionID, MultivaluedMap<String, String> params) throws Exception {		
		BackendDataTable table = dataTableRegistry.getTable(collectionID);
		
		List<Bson> queryFragments = new ArrayList<>();
		for(String key:params.keySet()) {
			Matcher m = columnSearchPattern.matcher(key);
			Matcher searchMatcher = searchPattern.matcher(key);
			if(m.matches()) {
				int columnID = Integer.parseInt(m.group(1));
				ColumnDef column = table.getColumnByID(columnID);
				String searchValue = params.getFirst(key);

				if(searchValue!=null && searchValue.length()>0) {
					if(column.getQueryFactory()!=null) {
						queryFragments.add(column.getQueryFactory().createQuery(column.getValue(), searchValue));
					}
				}
			} else if(searchMatcher.matches()) {
				String searchValue = params.getFirst(key);
				if(searchValue!=null && searchValue.length()>0) {
					// TODO implement full text search
				}
			}
		}
		
		int draw = Integer.parseInt(params.getFirst("draw"));
		
		int skip = Integer.parseInt(params.getFirst("start"));
		int limit = Integer.parseInt(params.getFirst("length"));
		
		int sortColumnID = Integer.parseInt(params.getFirst("order[0][column]"));
		ColumnDef sortColumn = table.getColumnByID(sortColumnID);
		String sortDir = params.getFirst("order[0][dir]");
		SearchOrder order = new SearchOrder(sortColumn.getValue(), sortDir.equals("asc")?1:-1);
		
		if(table.getQuery()!=null) {
			JsonObject filter = null;
			if(params.containsKey("params")) {
				JsonReader reader = Json.createReader(new StringReader(params.getFirst("params")));
				filter = reader.readObject();
				
			}
			Bson fragment = table.getQuery().buildAdditionalQuery(filter);
			if(fragment!=null) {
				queryFragments.add(fragment);				
			}
		}
		
		Bson query = queryFragments.size()>0?Filters.and(queryFragments):new Document();
		
		if(params.containsKey("export")) {
			String reportID = params.getFirst("export");
			exportTaskManager.createExportTask(reportID, new ExportTask(table, query, order));
		}
		
		CollectionFind<Document> find = table.getCollection().find(query, order, skip, limit);
		
		Iterator<Document> it = find.getIterator();
		List<Document> objects = new ArrayList<>();	
		while(it.hasNext()) {
			objects.add(it.next());
		}
		
		String[][] data = new String[objects.size()][table.getColumns().size()];
		for(int i = 0; i<objects.size();i++) {
			Document row = objects.get(i);
			String[] rowFormatted = formatRow(table.getColumns(), row);
			data[i] = rowFormatted;
		}
		BackendDataTableDataResponse response = new BackendDataTableDataResponse(draw, find.getRecordsTotal(), find.getRecordsFiltered(), data);
		
		return response;
	}

	private static String[] formatRow(List<ColumnDef> columns, Document row) {
		int columnID = 0;
		String[] rowFormatted = new String[columns.size()];
		for(ColumnDef column:columns) {
			if(column.getValue()!=null) {
				String[] keys = column.getValue().split("\\.");
				
				Object value = row;
				for(String key:keys) {
					if(value!=null) {
						value = ((Document)value).get(key);
					}
				}
				rowFormatted[columnID] = value!=null?format(value,row,column):"";
			} else {
				rowFormatted[columnID] = format(null,row,column);
			}
			columnID++; 
		}
		return rowFormatted;
	}
	
	@GET
	@Path("/exports/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public ExportStatus getExport(@PathParam("id") String reportID) throws Exception {
		return exportTaskManager.getExportStatus(reportID);
	}
	
	private static final String CSV_DELIMITER = ";";
	
	public static class ExportTask extends ExportRunnable {
		
		protected BackendDataTable table;
		protected Bson query;
		protected SearchOrder order;
		
		public ExportTask(BackendDataTable table, Bson query, SearchOrder order) {
			super();
			this.table = table;
			this.query = query;
			this.order = order;
		}

		protected void runExport() throws Exception {			
			try {
				CollectionFind<Document> find = table.getCollection().find(query, order, null, null);		
	
				PrintWriter writer = new PrintWriter(new File(getContainer()+"/export.csv"),"UTF-8");
				
				try {
					List<ColumnDef> columns = table.getExportColumns()!=null?table.getExportColumns():table.getColumns();
					
					for(ColumnDef colDef:columns) {
						// Workaround for Excel bug. "SYLK: File format is not valid" 
						String title = colDef.title.replaceAll("^ID", "id");
						writer.print(title);
						writer.print(CSV_DELIMITER);
					}
					writer.println();
					
					find.getRecordsFiltered();
					
					Iterator<Document> it = find.getIterator();
					
					int count = 0;
					while(it.hasNext()) {
						count++;
						Document object = it.next();
						String[] formattedRow = DataTableServices.formatRow(columns, object);
						for(String val:formattedRow) {
							if(val.contains(CSV_DELIMITER)||val.contains("\n")||val.contains("\"")) {
								val = "\"" + val.replaceAll("\"", "\"\"") + "\"";
							}
							writer.print(val);
							writer.print(CSV_DELIMITER);
						}
						writer.println();
						//status.progress = (float) (1.0 * count / find.getRecordsFiltered());
					}
				} finally {
					writer.close();
				}
				
			} catch (Exception e) {
				logger.error("An error occurred while generating report", e);
			}
		}
	}
	
	public static Field getField(String fieldName, Class<?> type) {
		try {
			return type.getDeclaredField(fieldName);
		} catch (NoSuchFieldException e) {
			if (type.getSuperclass() != null) {
		        return getField(fieldName, type.getSuperclass());
		    } else {
		    	return null;
		    }
		} catch (SecurityException e) {
			throw e;
		}
	}
	
	private static String format(Object value, Document row, ColumnDef column) {
		return column.format.format(value, row);
	}
}
