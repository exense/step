package step.plugins.datatable;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.AttachmentContainer;
import step.attachments.AttachmentManager;
import step.core.accessors.Collection;
import step.core.accessors.CollectionFind;
import step.core.accessors.SearchOrder;
import step.core.deployment.AbstractServices;
import step.core.execution.model.ExecutionStatus;
import step.plugins.screentemplating.Input;
import step.plugins.screentemplating.ScreenTemplatePlugin;

import com.mongodb.DBObject;
import com.mongodb.MongoClient;

@Singleton
@Path("datatable")
public class DataTableServices extends AbstractServices {
	
	private static final Logger logger = LoggerFactory.getLogger(DataTableServices.class);
		
	Map<String, BackendDataTable> tables = new HashMap<>();	
	
	Map<String, ReportStatus> reports = new ConcurrentHashMap<>();
	
	ExecutorService reportExecutor = Executors.newFixedThreadPool(2);
	
	@PostConstruct
	public void init() {
		MongoClient client = getContext().getMongoClient();
		
		BackendDataTable executions = new BackendDataTable(new Collection(client, "executions"));
		executions.addColumn("ID", "_id").addColumn("Description", "description").addDateColumn("Start time", "startTime")
		.addDateColumn("End time", "endTime").addColumn("User", "executionParameters.userID");
				
		ScreenTemplatePlugin screenTemplates = (ScreenTemplatePlugin) getContext().get(ScreenTemplatePlugin.SCREEN_TEMPLATE_KEY);
		if(screenTemplates!=null) {
			for(Input input:screenTemplates.getInputsForScreen("executionTable", null)) {
				executions.addColumn(input.getLabel(), input.getId());
			}
		}
		executions.addTextWithDropdownColumn("Status", "status", Arrays.asList(ExecutionStatus.values()).stream().map(Object::toString).collect(Collectors.toList()));
		
		ColumnBuilder leafReportNodesColumns = new ColumnBuilder();
		leafReportNodesColumns.addDateColumn("Begin", "executionTime").addColumn("Name","name").addColumn("Status","status").addColumn("Error", "error")
		.addColumn("Input","input").addColumn("Output","output").addColumn("Duration","duration").addColumn("Adapter", "adapter");

		BackendDataTable leafReportNodes = new BackendDataTable(new Collection(client, "reports"));
		leafReportNodes.addColumn("ID", "_id").addTimeColumn("Begin", "executionTime").addRowAsJson("Step","input","output","error","name")
		.addJsonColumn("Attachments", "attachments").addTextWithDropdownColumn("Status", "status").setQuery(new TestStepReportNodeFilter()).setExportColumns(leafReportNodesColumns.build());
		
		BackendDataTable artefactTable = new BackendDataTable(new Collection(client, "artefacts"));
		artefactTable.addColumn("ID", "_id").addColumn("Name", "name").addJsonColumn("Attachments", "attachments").addJsonColumn("Childrens", "childrenIDs");
		
		tables.put("executions", executions);
		tables.put("reports", leafReportNodes);
		tables.put("artefacts", artefactTable);		
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
	public List<ColumnDef> getTableColumnDefs(@PathParam("id") String collectionID) {
		BackendDataTable table = tables.get(collectionID);
		return table.getColumns();
	}
	
	@POST
	@Path("/{id}/data")
	@Consumes("application/x-www-form-urlencoded")
	@Produces(MediaType.APPLICATION_JSON)
	public BackendDataTableDataResponse getTableData_Post(@PathParam("id") String collectionID, MultivaluedMap<String, String> form) throws Exception {
		return getTableData(collectionID, form);
	}
	
	@GET
	@Path("/{id}/data")
	@Produces(MediaType.APPLICATION_JSON)
	public BackendDataTableDataResponse getTableData_Get(@PathParam("id") String collectionID, @Context UriInfo uriInfo) throws Exception {
		return getTableData(collectionID, uriInfo.getQueryParameters());
	}
	
	private BackendDataTableDataResponse getTableData(@PathParam("id") String collectionID, MultivaluedMap<String, String> params) throws Exception {		
		BackendDataTable table = tables.get(collectionID);
		
		List<String> queryFragments = new ArrayList<>();
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
		
		String additionalQuery;
		if(params.containsKey("params")) {
			JsonReader reader = Json.createReader(new StringReader(params.getFirst("params")));
			JsonObject filter = reader.readObject();
			additionalQuery = table.getQuery().buildAdditionalQuery(filter);
			queryFragments.add(additionalQuery);
		} 
		
		if(params.containsKey("export")) {
			String reportID = params.getFirst("export");
			ReportStatus status = new ReportStatus();
			reports.put(reportID, status);
			reportExecutor.execute(new Runnable() {			
				@Override
				public void run() {
					export(reportID, table, queryFragments, order);					
				}
			});
		}
		
		CollectionFind<DBObject> find = table.getCollection().find(queryFragments, order, skip, limit);
		
		Iterator<DBObject> it = find.getIterator();
		List<DBObject> objects = new ArrayList<>();	
		while(it.hasNext()) {
			objects.add(it.next());
		}
		
		String[][] data = new String[objects.size()][table.getColumns().size()];
		for(int i = 0; i<objects.size();i++) {
			DBObject row = objects.get(i);
			String[] rowFormatted = formatRow(table.getColumns(), row);
			data[i] = rowFormatted;
		}
		BackendDataTableDataResponse response = new BackendDataTableDataResponse(draw, find.getRecordsTotal(), find.getRecordsFiltered(), data);
		
		return response;
	}

	private String[] formatRow(List<ColumnDef> columns, DBObject row) {
		int columnID = 0;
		String[] rowFormatted = new String[columns.size()];
		for(ColumnDef column:columns) {
			if(column.getValue()!=null) {
				String[] keys = column.getValue().split("\\.");
				
				Object value = row;
				for(String key:keys) {
					value = ((DBObject)value).get(key);
				}
				
				rowFormatted[columnID] = value!=null?format(value,row,column):"";
			} else {
				rowFormatted[columnID] = format(null,row,column);
			}
			columnID++; 
		}
		return rowFormatted;
	}
	
	public class ReportStatus {
		
		String attachmentID;
		
		volatile boolean ready = false;
				
		volatile float progress = 0;

		public ReportStatus() {
			super();
		}

		public String getAttachmentID() {
			return attachmentID;
		}

		public void setAttachmentID(String attachmentID) {
			this.attachmentID = attachmentID;
		}

		public boolean isReady() {
			return ready;
		}

		public void setReady(boolean ready) {
			this.ready = ready;
		}

		public float getProgress() {
			return progress;
		}

		public void setProgress(float progress) {
			this.progress = progress;
		}
	}
	
	@GET
	@Path("/exports/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public ReportStatus getExport(@PathParam("id") String reportID) throws Exception {
		ReportStatus report = reports.get(reportID);
		if(report.ready) {
			reports.remove(reportID);
		}
		return report;
	}
	
	private static final String CSV_DELIMITER = ";";
	
	private void export(String id, BackendDataTable table, List<String> queryFragments, SearchOrder order) {
		AttachmentContainer container = AttachmentManager.createAttachmentContainer();
		ReportStatus status = reports.get(id);
		status.setAttachmentID(container.getMeta().getId().toString());
		
		try {
			CollectionFind<DBObject> find = table.getCollection().find(queryFragments, order, null, null);		

			PrintWriter writer = new PrintWriter(new File(container.getContainer().getAbsoluteFile()+"/export.csv"),"UTF-8");
			
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
				
				Iterator<DBObject> it = find.getIterator();
				
				int count = 0;
				while(it.hasNext()) {
					count++;
					DBObject object = it.next();
					String[] formattedRow = formatRow(columns, object);
					for(String val:formattedRow) {
						if(val.contains(CSV_DELIMITER)||val.contains("\n")||val.contains("\"")) {
							val = "\"" + val.replaceAll("\"", "\"\"") + "\"";
						}
						writer.print(val);
						writer.print(CSV_DELIMITER);
					}
					writer.println();
					status.progress = (float) (1.0 * count / find.getRecordsFiltered());
				}
			} finally {
				writer.close();
			}
			
		} catch (Exception e) {
			logger.error("An error occurred while generating report", e);
		} finally {
			status.setProgress(1);
			status.setReady(true);
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
	
	private String format(Object value, DBObject row, ColumnDef column) {
		return column.format.format(value, row);
	}
}
