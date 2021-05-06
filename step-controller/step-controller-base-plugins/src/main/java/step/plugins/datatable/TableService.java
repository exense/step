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
package step.plugins.datatable;

import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.accessors.collections.CollectionRegistry;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.collections.SearchOrder;
import step.core.deployment.ApplicationServices;
import step.core.deployment.JacksonMapperProvider;
import step.core.deployment.Secured;
import step.core.export.ExportTaskManager;
import step.core.export.ExportTaskManager.ExportRunnable;
import step.core.export.ExportTaskManager.ExportStatus;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.ql.OQLFilterBuilder;
import step.core.tables.Table;
import step.core.tables.TableColumn;
import step.core.tables.TableFindResult;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionContainer;

@Singleton
@Path("table")
public class TableService extends ApplicationServices {
	
	private static final Logger logger = LoggerFactory.getLogger(TableService.class);
	
	private ObjectHookRegistry objectHookRegistry;
	protected CollectionRegistry collectionRegistry;
	protected int maxTime;
	
	protected ExportTaskManager exportTaskManager;

	private ObjectMapper webLayerObjectMapper = JacksonMapperProvider.createMapper();
	
	private Pattern columnSearchPattern = Pattern.compile("columns\\[([0-9]+)\\]\\[search\\]\\[value\\]");
	private Pattern searchPattern = Pattern.compile("search\\[value\\]");
	private Pattern namePattern = Pattern.compile("columns\\[([0-9]+)\\]\\[name\\]");
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		collectionRegistry = getContext().get(CollectionRegistry.class);
		maxTime = controller.getContext().getConfiguration().getPropertyAsInteger("db.query.maxTime",30);
		objectHookRegistry = getContext().get(ObjectHookRegistry.class);
		exportTaskManager = new ExportTaskManager(getContext().getResourceManager());
	}
	
	@PreDestroy
	public void destroy() {
	}
	
	@POST
	@Path("/{id}/data")
	@Consumes("application/x-www-form-urlencoded")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public DataTableResponse getTableData_Post(@PathParam("id") String collectionID, MultivaluedMap<String, String> form, @Context UriInfo uriInfo) throws Exception {
		if(uriInfo.getQueryParameters()!=null) {
			form.putAll(uriInfo.getQueryParameters());
		}
		Filter sessionQueryFragment = getAdditionalQueryFragmentFromContext(collectionID);
		return getTableData(collectionID, form, sessionQueryFragment);
	}
	
	@GET
	@Path("/{id}/data")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public DataTableResponse getTableData_Get(@PathParam("id") String collectionID, @Context UriInfo uriInfo) throws Exception {
		Filter sessionQueryFragment = getAdditionalQueryFragmentFromContext(collectionID);
		return getTableData(collectionID, uriInfo.getQueryParameters(), sessionQueryFragment);
	}
	
	@GET
	@Path("/{id}/column/{column}/distinct")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public List<String> getTableColumnDistinct(@PathParam("id") String collectionID, @PathParam("column") String column, @Context UriInfo uriInfo) throws Exception {
		Table<?> collection = collectionRegistry.get(collectionID);
		return collection.distinct(column);
	}
	
	@POST
	@Path("/{id}/searchIdsBy/{column}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured
	public List<String> searchIdsBy(@PathParam("id") String collectionID, @PathParam("column") String columnName, String searchValue) throws Exception {
		Table<?> collection = collectionRegistry.get(collectionID);
		Filter columnQueryFragment = collection.getQueryFragmentForColumnSearch(columnName, searchValue);
		return collection.distinct("_id",columnQueryFragment);
	}
	
	private DataTableResponse getTableData(@PathParam("id") String collectionID, MultivaluedMap<String, String> params, Filter sessionQueryFragment) throws Exception {		
		Table<?> collection = collectionRegistry.get(collectionID);
		if(collection == null) {
			throw new RuntimeException("The collection "+collectionID+" doesn't exist");
		}

		Map<Integer, String> columnNames = getColumnNamesMap(params);
		
		List<Filter> queryFragments = createQueryFragments(params, columnNames, collection);
		
		int draw = Integer.parseInt(params.getFirst("draw"));
		int skip = Integer.parseInt(params.getFirst("start"));
		int limit = Integer.parseInt(params.getFirst("length"));
		
		int sortColumnID = Integer.parseInt(params.getFirst("order[0][column]"));
		String sortColumnName = columnNames.get(sortColumnID);
		
		String sortDir = params.getFirst("order[0][dir]");
		SearchOrder order;
		if(sortColumnName != null && !sortColumnName.isEmpty()) {
			order = new SearchOrder(sortColumnName, sortDir.equals("asc")?1:-1);
		} else {
			order = null;
		}
		
		if(collection.isFiltered() && sessionQueryFragment != null) {
			queryFragments.add(sessionQueryFragment);
		}
		
		JsonObject queryParameters = null;
		if(params.containsKey("params")) {
			JsonReader reader = Json.createReader(new StringReader(params.getFirst("params")));
			queryParameters = reader.readObject();
		}
		List<Filter> additionalQueryFragments = collection.getAdditionalQueryFragments(queryParameters);
		if(additionalQueryFragments != null) {
			queryFragments.addAll(additionalQueryFragments);
		}
		
		Filter query = queryFragments.size()>0?Filters.and(queryFragments):Filters.empty();
		
		TableFindResult<?> find = collection.find(query, order, skip, limit, maxTime);

		List<Object> objects = new ArrayList<>();
		Iterator<?> iterator = find.getIterator();
		while(iterator.hasNext()) {
			objects.add(iterator.next());
		}
		
		String[][] data = new String[objects.size()][1];
		for(int i = 0; i<objects.size(); i++) {
			Object row = objects.get(i);
			
			String[] rowFormatted = new String[columnNames.size()];
			try {
				String rowAsString = webLayerObjectMapper.writeValueAsString(row);
				rowFormatted[0] = rowAsString;
			} catch (Exception e) {
				logger.error("Error while serializing "+row, e);
				throw e;
			}
			
			data[i] = rowFormatted;
		}
		DataTableResponse response = new DataTableResponse(draw, find.getRecordsTotal(), find.getRecordsFiltered(), data);
		
		return response;
	}
	
	@GET
	@Path("/{id}/export")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public String createExport(@PathParam("id") String collectionID, @Context UriInfo uriInfo) throws Exception {
		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
	
		Table<?> collection = collectionRegistry.get(collectionID);
		if(collection == null) {
			throw new RuntimeException("The collection "+collectionID+" doesn't exist");
		}
		
		JsonObject queryParameters = null;
		if(params.containsKey("params")) {
			JsonReader reader = Json.createReader(new StringReader(params.getFirst("params")));
			queryParameters = reader.readObject();
		}
		
		List<Filter> queryFragments = collection.getAdditionalQueryFragments(queryParameters);
		Filter query = queryFragments.size()>0?Filters.and(queryFragments):Filters.empty();
		
		String exportID = UUID.randomUUID().toString();
		exportTaskManager.createExportTask(exportID, new ExportTask(collection, null, query));
		
		return "{\"exportID\":\"" + exportID + "\"}";
	}
	
	@GET
	@Path("/exports/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public ExportStatus getExport(@PathParam("id") String reportID) throws Exception {
		return exportTaskManager.getExportStatus(reportID);
	}
	

	private List<Filter> createQueryFragments(MultivaluedMap<String, String> params, Map<Integer, String> columnNames, Table<?> collection) {
		List<Filter> queryFragments = new ArrayList<>();
		for(String key:params.keySet()) {
			Matcher m = columnSearchPattern.matcher(key);
			Matcher searchMatcher = searchPattern.matcher(key);
			if(m.matches()) {
				int columnID = Integer.parseInt(m.group(1));
				String columnName = columnNames.get(columnID);
				String searchValue = params.getFirst(key);

				if(searchValue!=null && searchValue.length()>0) {
					Filter columnQueryFragment = collection.getQueryFragmentForColumnSearch(columnName, searchValue);
					queryFragments.add(columnQueryFragment);
				}
			} else if(searchMatcher.matches()) {
				String searchValue = params.getFirst(key);
				if(searchValue!=null && searchValue.length()>0) {
					// TODO implement full text search
				}
			}
		}
		if(params.containsKey("filter")) {
			Filter filter = OQLFilterBuilder.getFilter(params.getFirst("filter"));
			queryFragments.add(filter);
		}
		return queryFragments;
	}

	private Map<Integer, String> getColumnNamesMap(MultivaluedMap<String, String> params) {
		Map<Integer, String> columnNames = new HashMap<>();
		
		for(String key:params.keySet()) {
			Matcher m = namePattern.matcher(key);
			if(m.matches()) {
				int columnID = Integer.parseInt(m.group(1));
				String columnName = params.getFirst(key);
				columnNames.put(columnID, columnName);
			}
		}
		return columnNames;
	}

	private Filter getAdditionalQueryFragmentFromContext(String collectionID) {
		Filter query = OQLFilterBuilder.getFilter(objectHookRegistry.getObjectFilter(getSession()).getOQLFilter());
		return query;
	}

	public static class ExportTask extends ExportRunnable {

		protected Table<?> collection;
		protected Map<String, TableColumn> columns;
		protected Filter query;

		public ExportTask(Table<?> collection, Map<String, TableColumn> columns, Filter query) {
			super();
			this.collection = collection;
			this.columns = columns;
			this.query = query;
		}

		protected Resource runExport() throws Exception {
			try {
				ResourceRevisionContainer resourceContainer = getResourceManager().createResourceContainer(ResourceManager.RESOURCE_TYPE_TEMP, "export.csv");
				PrintWriter writer = new PrintWriter(resourceContainer.getOutputStream());

				try {
					collection.export(query, columns,writer);
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
	
}
