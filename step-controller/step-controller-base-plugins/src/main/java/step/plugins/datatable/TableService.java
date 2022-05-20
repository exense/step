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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.collections.SearchOrder;
import step.core.deployment.ApplicationServices;
import step.framework.server.security.Secured;
import step.core.export.ExportTaskManager;
import step.core.export.ExportTaskManager.ExportRunnable;
import step.core.export.ExportTaskManager.ExportStatus;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.ql.OQLFilterBuilder;
import step.core.tables.Table;
import step.core.tables.TableColumn;
import step.core.tables.TableFindResult;
import step.core.tables.TableRegistry;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionContainer;

@Singleton
@Path("table")
@Tag(name = "Tables")
public class TableService extends ApplicationServices {
	
	private static final Logger logger = LoggerFactory.getLogger(TableService.class);
	
	private ObjectHookRegistry objectHookRegistry;
	protected TableRegistry tableRegistry;
	protected int maxTime;
	
	protected ExportTaskManager exportTaskManager;

	private ObjectMapper webLayerObjectMapper = DefaultJacksonMapperProvider.getObjectMapper();
	
	private TableServiceHelper tableServiceHelper;
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		tableRegistry = getContext().get(TableRegistry.class);
		maxTime = getContext().getConfiguration().getPropertyAsInteger("db.query.maxTime",30);
		objectHookRegistry = getContext().get(ObjectHookRegistry.class);
		exportTaskManager = new ExportTaskManager(getContext().getResourceManager());
		tableServiceHelper = new TableServiceHelper();
	}
	
	@PreDestroy
	public void destroy() {
	}
	
	@POST
	@Path("/{id}/data")
	@Consumes("application/x-www-form-urlencoded")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public DataTableResponse getTableData_Post(@PathParam("id") String tableID, MultivaluedMap<String, String> form, @Context UriInfo uriInfo) throws Exception {
		if(uriInfo.getQueryParameters()!=null) {
			form.putAll(uriInfo.getQueryParameters());
		}
		Filter sessionQueryFragment = getAdditionalQueryFragmentFromContext(tableID);
		return getTableData(tableID, form, sessionQueryFragment);
	}
	
	@GET
	@Path("/{id}/data")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public DataTableResponse getTableData_Get(@PathParam("id") String tableID, @Context UriInfo uriInfo) throws Exception {
		Filter sessionQueryFragment = getAdditionalQueryFragmentFromContext(tableID);
		return getTableData(tableID, uriInfo.getQueryParameters(), sessionQueryFragment);
	}
	
	@GET
	@Path("/{id}/column/{column}/distinct")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public List<String> getTableColumnDistinct(@PathParam("id") String tableID, @PathParam("column") String column, @Context UriInfo uriInfo) throws Exception {
		Table<?> table = tableRegistry.get(tableID);
		return table.distinct(column);
	}
	
	@POST
	@Path("/{id}/searchIdsBy/{column}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured
	public List<String> searchIdsBy(@PathParam("id") String tableID, @PathParam("column") String columnName, String searchValue) throws Exception {
		Table<?> table = tableRegistry.get(tableID);
		Filter columnQueryFragment = table.getQueryFragmentForColumnSearch(columnName, searchValue);
		return table.distinct(AbstractIdentifiableObject.ID, columnQueryFragment);
	}
	
	private DataTableResponse getTableData(@PathParam("id") String tableID, MultivaluedMap<String, String> params, Filter sessionQueryFragment) throws Exception {		
		Table<?> table = tableRegistry.get(tableID);
		if(table == null) {
			throw new RuntimeException("The table "+tableID+" doesn't exist");
		}

		Map<Integer, String> columnNames = tableServiceHelper.getColumnNamesMap(params);
		
		List<Filter> queryFragments = tableServiceHelper.createQueryFragments(params, columnNames, table);
		
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
		
		if(table.isFiltered() && sessionQueryFragment != null) {
			queryFragments.add(sessionQueryFragment);
		}
		
		JsonObject queryParameters = null;
		if(params.containsKey("params")) {
			JsonReader reader = Json.createReader(new StringReader(params.getFirst("params")));
			queryParameters = reader.readObject();
		}
		List<Filter> additionalQueryFragments = table.getAdditionalQueryFragments(queryParameters);
		if(additionalQueryFragments != null) {
			queryFragments.addAll(additionalQueryFragments);
		}
		
		Filter query = queryFragments.size()>0?Filters.and(queryFragments):Filters.empty();
		
		TableFindResult<?> find = table.find(query, order, skip, limit, maxTime);

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
	public String createExport(@PathParam("id") String tableID, @Context UriInfo uriInfo) throws Exception {
		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
	
		Table<?> table = tableRegistry.get(tableID);
		if(table == null) {
			throw new RuntimeException("The table "+tableID+" doesn't exist");
		}
		
		JsonObject queryParameters = null;
		if(params.containsKey("params")) {
			JsonReader reader = Json.createReader(new StringReader(params.getFirst("params")));
			queryParameters = reader.readObject();
		}
		
		List<Filter> queryFragments = table.getAdditionalQueryFragments(queryParameters);
		Filter query = queryFragments.size()>0?Filters.and(queryFragments):Filters.empty();
		
		String exportID = UUID.randomUUID().toString();
		exportTaskManager.createExportTask(exportID, new ExportTask(table, null, query));
		
		return "{\"exportID\":\"" + exportID + "\"}";
	}
	
	@GET
	@Path("/exports/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public ExportStatus getExport(@PathParam("id") String reportID) throws Exception {
		return exportTaskManager.getExportStatus(reportID);
	}

	private Filter getAdditionalQueryFragmentFromContext(String tableID) {
		Filter query = OQLFilterBuilder.getFilter(objectHookRegistry.getObjectFilter(getSession()).getOQLFilter());
		return query;
	}

	public static class ExportTask extends ExportRunnable {

		private static final String END_OF_LINE = "\n";
		private static final String DELIMITER = ";";
		protected Table<?> table;
		protected Map<String, TableColumn> columns;
		protected Filter query;

		public ExportTask(Table<?> table, Map<String, TableColumn> columns, Filter query) {
			super();
			this.table = table;
			this.columns = columns;
			this.query = query;
		}

		protected Resource runExport() throws Exception {
			try {
				ResourceRevisionContainer resourceContainer = getResourceManager().createResourceContainer(ResourceManager.RESOURCE_TYPE_TEMP, "export.csv");
				PrintWriter writer = new PrintWriter(resourceContainer.getOutputStream());

				Map<String, TableColumn> exportFields = table.getExportFields();
				
				// Write headers
				exportFields.forEach((key, value) -> writer.append(value.getTitle()).append(DELIMITER));
				writer.append(END_OF_LINE);
				try {
					TableFindResult<?> find = table.find(query, null, null, null, 0);
					find.getIterator().forEachRemaining(o -> {
						// Write row
						exportFields.forEach((key, value) -> {
							Object property;
							String formattedValue;
							try {
								property = PropertyUtils.getProperty(o, key);
								if(property != null) {
									formattedValue = value.getFormat().format(property);
								} else {
									formattedValue = "";
								}
							} catch (NoSuchMethodException e) {
								formattedValue = "";
							} catch (IllegalAccessException | InvocationTargetException e) {
								throw new RuntimeException("Error while writing column "+key, e);
							}
							writer.append(formattedValue).append(DELIMITER);
						});
						writer.append(END_OF_LINE);
					});
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
