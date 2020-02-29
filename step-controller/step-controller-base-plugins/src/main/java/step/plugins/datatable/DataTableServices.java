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

import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import com.mongodb.DBObject;
import com.mongodb.client.model.Filters;

import step.core.accessors.CollectionFind;
import step.core.accessors.SearchOrder;
import step.core.deployment.Secured;
import step.core.export.ExportTaskManager;
import step.core.export.ExportTaskManager.ExportRunnable;
import step.core.export.ExportTaskManager.ExportStatus;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionContainer;

@Singleton
@Path("datatable")
public class DataTableServices extends AbstractTableService {

	private static final Logger logger = LoggerFactory.getLogger(DataTableServices.class);

	protected DataTableRegistry dataTableRegistry;

	protected ExportTaskManager exportTaskManager;

	protected final ExecutorService reportExecutor = Executors.newFixedThreadPool(2);
	
	protected int maxTime; 

	@PostConstruct
	public void init() throws Exception {
		super.init();
		exportTaskManager = new ExportTaskManager(getContext().get(ResourceManager.class));
		dataTableRegistry = getContext().get(DataTableRegistry.class);
		maxTime = controller.getContext().getConfiguration().getPropertyAsInteger("db.query.maxTime",30);
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
		if(table == null) {
			throw new RuntimeException("Unable to load table definition for entity: " + collectionID);
		}
		return table.getColumns();
	}

	@POST
	@Path("/{id}/data")
	@Consumes("application/x-www-form-urlencoded")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public BackendDataTableDataResponse getTableData_Post(@PathParam("id") String collectionID, MultivaluedMap<String, String> form) throws Exception {
		List<Bson> sessionQueryFragments = getAdditionalQueryFragmentsFromContext(collectionID);
		return getTableData(collectionID, form, sessionQueryFragments);
	}

	@GET
	@Path("/{id}/data")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public BackendDataTableDataResponse getTableData_Get(@PathParam("id") String collectionID, @Context UriInfo uriInfo) throws Exception {
		List<Bson> sessionQueryFragments = getAdditionalQueryFragmentsFromContext(collectionID);
		return getTableData(collectionID, uriInfo.getQueryParameters(), sessionQueryFragments);
	}

	private BackendDataTableDataResponse getTableData(@PathParam("id") String collectionID, MultivaluedMap<String, String> params, List<Bson> sessionQueryFragments) throws Exception {		
		BackendDataTable table = dataTableRegistry.getTable(collectionID);

		List<Bson> queryFragments = new ArrayList<>();
		for(String key:params.keySet()) {
			Matcher m = columnSearchPattern.matcher(key);
			Matcher searchMatcher = searchPattern.matcher(key);
			if(m.matches()) {
				int columnID = Integer.parseInt(m.group(1));
				ColumnDef column = null;
				try{
					column = table.getColumnByID(columnID);
					String searchValue = params.getFirst(key);

					if(searchValue!=null && searchValue.length()>0) {
						if(column.getQueryFactory()!=null) {
							queryFragments.add(column.getQueryFactory().createQuery(column.getValue(), searchValue));
						}
					}
				}catch(IndexOutOfBoundsException e) {
					// Column is unsupported on the server side but may be useful on the client
					// ignore until better support of client-side columns is implemented
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

		SearchOrder order = null;
		if(sortColumn.getValue() != null) {
			order = new SearchOrder(sortColumn.getValue(), sortDir.equals("asc")?1:-1);
		}

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

		if(table.isFiltered() && sessionQueryFragments != null) {
			queryFragments.addAll(sessionQueryFragments);
		}

		Bson query = queryFragments.size()>0?Filters.and(queryFragments):new Document();

		if(params.containsKey("export")) {
			String reportID = params.getFirst("export");
			exportTaskManager.createExportTask(reportID, new ExportTask(table, query, order));
		}

		CollectionFind<DBObject> find = table.getCollection().find(query, order, skip, limit, maxTime);

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

	private static String[] formatRow(List<ColumnDef> columns, DBObject row) {
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

		protected Resource runExport() throws Exception {			
			try {
				CollectionFind<DBObject> find = table.getCollection().find(query, order, null, null);		

				ResourceRevisionContainer resourceContainer = getResourceManager().createResourceContainer(ResourceManager.RESOURCE_TYPE_TEMP, "export.csv");

				PrintWriter writer = new PrintWriter(resourceContainer.getOutputStream());

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
					resourceContainer.save(null);
				}

				return resourceContainer.getResource();

			} catch (Exception e) {
				logger.error("An error occurred while generating report", e);
				throw e;
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

	private static String format(Object value, DBObject row, ColumnDef column) {
		return column.format.format(value, row);
	}
}
