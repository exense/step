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
package step.core.accessors.collections;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.jongo.Mapper;
import org.jongo.marshall.Unmarshaller;
import org.jongo.marshall.jackson.JacksonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoExecutionTimeoutException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Filters;

import step.core.accessors.AccessorLayerJacksonMapperProvider;
import step.core.accessors.collections.field.CollectionField;
import step.core.accessors.collections.field.formatter.StringFormatter;
import step.core.objectenricher.ObjectFilter;
import step.core.objectenricher.ObjectHookRegistry;

public class Collection<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(Collection.class);

	private static final int DEFAULT_LIMIT = 1000;
	
	protected static final String CSV_DELIMITER = ";";
	
	private final boolean filtered;
	
	private final Class<T> entityClass;

	private MongoCollection<BasicDBObject> collection;

	private Mapper dbLayerObjectMapper;
	
	/**
	 * @param mongoDatabase
	 * @param collectionName the name of the mongo collection
	 * @param entityClass the 
	 * @param filtered if the {@link Collection} is subject to context filtering i.e. 
	 * if the context parameters delivered by the {@link ObjectFilter}s of the {@link ObjectHookRegistry}
	 * may be appended to the queries run against this collection
	 */
	public Collection(MongoDatabase mongoDatabase, String collectionName, Class<T> entityClass, boolean filtered) {
		this.filtered = filtered;
		this.entityClass = entityClass;
		
		collection = mongoDatabase.getCollection(collectionName, BasicDBObject.class);

		JacksonMapper.Builder builder2 = new JacksonMapper.Builder();
		AccessorLayerJacksonMapperProvider.getModules().forEach(m->builder2.registerModule(m));
		dbLayerObjectMapper = builder2.build();
		
	}

	/**
	 * @return true if the filter defined by the {@link ObjectFilter} of the {@link ObjectHookRegistry} have to be applied 
	 * when performing a search
	 */
	public boolean isFiltered() {
		return filtered;
	}

	/**
	 * @param columnName the name of the column (field)
	 * @return the distinct values of the column 
	 */
	public List<String> distinct(String columnName) {
		return collection.distinct(columnName, String.class).filter(new Document(columnName,new Document("$ne",null))).into(new ArrayList<String>());
	}
	
	public CollectionFind<T> find(Bson query, SearchOrder order, Integer skip, Integer limit) {
		return this.find(query, order, skip, limit,0);
	}

	public CollectionFind<T> find(Bson query, SearchOrder order, Integer skip, Integer limit, int maxTime) {
		long count = collection.estimatedDocumentCount();
		
		CountOptions option = new CountOptions();
		option.skip(0).limit(DEFAULT_LIMIT);
		long countResults = collection.countDocuments(query, option);
			
		FindIterable<BasicDBObject> find = collection.find(query).maxTime(maxTime, TimeUnit.SECONDS);
		if(order!=null) {
			Document sortDoc = new Document(order.getAttributeName(), order.getOrder());
			find.sort(sortDoc);
		}
		if(skip!=null) {
			find.skip(skip);
		}
		if(limit!=null) {
			find.limit(limit);
		}
		MongoCursor<BasicDBObject> iterator;
		try {
			iterator = find.iterator();
		} catch (MongoExecutionTimeoutException e) {
			logger.error("Query execution exceeded timeout of " + maxTime + " " + TimeUnit.SECONDS);
			throw e;
		}
		
		Unmarshaller unmarshaller = dbLayerObjectMapper.getUnmarshaller();
		Iterator<T> enrichedIterator = new Iterator<T>() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public T next() {
				BasicDBObject next = iterator.next();
				T entity = unmarshaller.unmarshall(org.jongo.bson.Bson.createDocument(next), entityClass);
				entity = enrichEntity(entity);
				return entity;
			}
		};
		
		CollectionFind<T> collectionFind = new CollectionFind<>(count, countResults, enrichedIterator);
		return collectionFind;
	}
	
	/**
	 * @param queryParameters some context parameters that might be required to generate the additional query fragments
	 * @return a list of query fragments to be appended to the queries when calling the method find()
	 */
	public List<Bson> getAdditionalQueryFragments(JsonObject queryParameters) {
		return null;
	}

	/**
	 * @param columnName the name of the column on which the search is applied
	 * @param searchValue the value entered by the end user 
	 * @return a list of query fragments to be appended to the queries when performing a column search
	 */
	public Bson getQueryFragmentForColumnSearch(String columnName, String searchValue) {
		return Filters.regex(columnName, searchValue);
	}
	
	public Class<?> getEntityClass() {
		return entityClass;
	}

	/**
	 * This hook can be called for each element returned by the find() methods and
	 * allows enrichment of the returned objects
	 *  
	 * @param element the element to be modified
	 * @return the modified element
	 */
	protected T enrichEntity(T element) {
		return element;
	}
	
	/**
	 * Export data to CSV
	 * @param query
	 * @param columns
	 * @param writer
	 */
	public void export(Bson query, Map<String, CollectionField> columns, PrintWriter writer) {
		FindIterable<BasicDBObject> find = collection.find(query);
		MongoCursor<BasicDBObject> iterator;
		iterator = find.iterator();
		if (!iterator.hasNext()) {
			return;
		}
		BasicDBObject basicDBObject = iterator.next();
		//if column names not provided by the caller, get them from the collection
		if (columns == null || columns.size() == 0) {
			columns = getExportFields();
		}
		//if the collection has also no specification, dump all keys found in first object
		if (columns == null || columns.size() == 0 && iterator.hasNext()) {
			columns = getExportFields();		
			for (String key : basicDBObject.keySet()) {
				columns.put(key, new CollectionField(key,key));			
			}
		}
		//write headers
		columns.values().forEach((v)->{
			String title = v.getTitle().replaceAll("^ID", "id");
			writer.print(title);
			writer.print(CSV_DELIMITER);
		});
		writer.println();
		//Dump first row (required when writting all keys found in 1st object)
		dumpRow(basicDBObject,columns,writer);

		int count = 1;
		while(iterator.hasNext()) {
			count++;
			basicDBObject = iterator.next();
			dumpRow(basicDBObject,columns,writer);
		}
	}
	
	private void dumpRow(BasicDBObject basicDBObject, Map<String, CollectionField> fields, PrintWriter writer) {
		fields.forEach((key,field)->{
			Object value = basicDBObject.get(key);
			if (value != null) {
				String valueStr = field.getFormat().format(value);
				if(valueStr.contains(CSV_DELIMITER)||valueStr.contains("\n")||valueStr.contains("\"")) {
					valueStr = "\"" + valueStr.replaceAll("\"", "\"\"") + "\"";
				}
				writer.print(valueStr);
			}
			writer.print(CSV_DELIMITER);
		});
		writer.println();
	}

	protected Map<String, CollectionField> getExportFields() {
		Map<String, CollectionField> result = new HashMap<String,CollectionField> ();
		return result;
	}
	
}
