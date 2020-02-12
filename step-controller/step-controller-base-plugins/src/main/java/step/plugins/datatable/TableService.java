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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
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
import step.core.accessors.CollectionRegistry;
import step.core.accessors.SearchOrder;
import step.core.deployment.Secured;
import step.core.export.ExportTaskManager;
import step.core.ql.OQLMongoDBBuilder;

@Singleton
@Path("table")
public class TableService extends AbstractTableService {
	
	private static final Logger logger = LoggerFactory.getLogger(TableService.class);
	
	protected CollectionRegistry collectionRegistry;
	
	protected ExportTaskManager exportTaskManager;
	
	ExecutorService reportExecutor = Executors.newFixedThreadPool(2);
	
	protected MongoDatabase database;
	
	protected int maxTime;
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		database = getContext().getMongoClientSession().getMongoDatabase();
		collectionRegistry = getContext().get(CollectionRegistry.class);
		maxTime = controller.getContext().getConfiguration().getPropertyAsInteger("db.query.maxTime",30);
	}
	
	@PreDestroy
	public void destroy() {
	}
	
	Pattern columnSearchPattern = Pattern.compile("columns\\[([0-9]+)\\]\\[search\\]\\[value\\]");
	Pattern searchPattern = Pattern.compile("search\\[value\\]");
	Pattern namePattern = Pattern.compile("columns\\[([0-9]+)\\]\\[name\\]");
	
	@POST
	@Path("/{id}/data")
	@Consumes("application/x-www-form-urlencoded")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured
	public BackendDataTableDataResponse getTableData_Post(@PathParam("id") String collectionID, MultivaluedMap<String, String> form, @Context UriInfo uriInfo) throws Exception {
		if(uriInfo.getQueryParameters()!=null) {
			form.putAll(uriInfo.getQueryParameters());
		}
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
		Collection collection = collectionRegistry.get(collectionID);
		if(collection==null) {
			// no custom collection. use default collection
			collection = new Collection(database, collectionID);
		}
		
		Map<Integer, String> columnNames = getColumnNamesMap(params);
		
		List<Bson> queryFragments = createQueryFragments(params, columnNames);
		
		int draw = Integer.parseInt(params.getFirst("draw"));
		int skip = Integer.parseInt(params.getFirst("start"));
		int limit = Integer.parseInt(params.getFirst("length"));
		
		int sortColumnID = Integer.parseInt(params.getFirst("order[0][column]"));
		String sortColumnName = columnNames.get(sortColumnID);
		
		String sortDir = params.getFirst("order[0][dir]");
		SearchOrder order = new SearchOrder(sortColumnName, sortDir.equals("asc")?1:-1);
		
		if(collection.isFiltered() && sessionQueryFragments != null) {
			queryFragments.addAll(sessionQueryFragments);
		}
		
		List<Bson> additionalQueryFragments = collection.getAdditionalQueryFragments();
		if(additionalQueryFragments != null) {
			queryFragments.addAll(additionalQueryFragments);
		}
		
		Bson query = queryFragments.size()>0?Filters.and(queryFragments):new Document();
		
		
		CollectionFind<Document> find = collection.find(query, order, skip, limit, maxTime);
		
		Iterator<Document> it = find.getIterator();
		List<Document> objects = new ArrayList<>();	
		while(it.hasNext()) {
			objects.add(it.next());
		}
		
		String[][] data = new String[objects.size()][1];
		for(int i = 0; i<objects.size();i++) {
			Document row = objects.get(i);
			String[] rowFormatted = new String[columnNames.size()];
			rowFormatted[0] = row.toJson();
			data[i] = rowFormatted;
		}
		BackendDataTableDataResponse response = new BackendDataTableDataResponse(draw, find.getRecordsTotal(), find.getRecordsFiltered(), data);
		
		return response;
	}

	private List<Bson> createQueryFragments(MultivaluedMap<String, String> params, Map<Integer, String> columnNames) {
		List<Bson> queryFragments = new ArrayList<>();
		for(String key:params.keySet()) {
			Matcher m = columnSearchPattern.matcher(key);
			Matcher searchMatcher = searchPattern.matcher(key);
			if(m.matches()) {
				int columnID = Integer.parseInt(m.group(1));
				String columnName = columnNames.get(columnID);
				String searchValue = params.getFirst(key);

				if(searchValue!=null && searchValue.length()>0) {
					queryFragments.add(Filters.regex(columnName, searchValue));
				}
			} else if(searchMatcher.matches()) {
				String searchValue = params.getFirst(key);
				if(searchValue!=null && searchValue.length()>0) {
					// TODO implement full text search
				}
			}
		}
		if(params.containsKey("filter")) {
			Bson filter = OQLMongoDBBuilder.build(params.getFirst("filter"));
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
}
