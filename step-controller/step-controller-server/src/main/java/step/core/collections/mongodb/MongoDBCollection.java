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
package step.core.collections.mongodb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.jongo.Mapper;
import org.jongo.marshall.Unmarshaller;
import org.jongo.marshall.jackson.JacksonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Streams;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoExecutionTimeoutException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.CountOptions;

import step.core.accessors.AccessorLayerJacksonMapperProvider;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.SearchOrder;
import step.core.objectenricher.ObjectFilter;
import step.core.objectenricher.ObjectHookRegistry;

public class MongoDBCollection<T> implements Collection<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(MongoDBCollection.class);

	private static final int DEFAULT_LIMIT = 1000;
	
	protected static final String CSV_DELIMITER = ";";
	
	private final Class<T> entityClass;

	private MongoCollection<BasicDBObject> collection;
	protected org.jongo.MongoCollection jongoCollection;

	private Mapper dbLayerObjectMapper;
	
	/**
	 * @param mongoDatabase
	 * @param collectionName the name of the mongo collection
	 * @param entityClass the 
	 * @param filtered if the {@link MongoDBCollection} is subject to context filtering i.e. 
	 * if the context parameters delivered by the {@link ObjectFilter}s of the {@link ObjectHookRegistry}
	 * may be appended to the queries run against this collection
	 */
	public MongoDBCollection(MongoClientSession mongoClientSession, String collectionName, Class<T> entityClass) {
		this.entityClass = entityClass;
		
		collection = mongoClientSession.getMongoDatabase().getCollection(collectionName, BasicDBObject.class);
		jongoCollection = mongoClientSession.getJongoCollection(collectionName);

		JacksonMapper.Builder builder2 = new JacksonMapper.Builder();
		AccessorLayerJacksonMapperProvider.getModules().forEach(m->builder2.registerModule(m));
		dbLayerObjectMapper = builder2.build();
		
	}

	/**
	 * @param columnName the name of the column (field)
	 * @param query: the query filter
	 * @return the distinct values of the column 
	 */
	@Override
	public List<String> distinct(String columnName, Filter filter) {
		Bson query = filterToQuery(filter);
		
		if (columnName.equals("_id")) {
			return collection.distinct(columnName, query, ObjectId.class).map(ObjectId::toString).into(new ArrayList<String>());
		} else {
			return collection.distinct(columnName, query, String.class).into(new ArrayList<String>());
		}
	}

	/**
	 * @param columnName the name of the column (field)
	 * @return the distinct values of the column 
	 */
	@Override
	public List<String> distinct(String columnName) {
		return collection.distinct(columnName, String.class).filter(new Document(columnName,new Document("$ne",null))).into(new ArrayList<String>());
	}
	
	private Bson filterToQuery(Filter filter) {
		return new MongoDBFilterFactory().buildFilter(filter);
	}
	
	@Override
	public Stream<T> find(Filter filter, SearchOrder order, Integer skip, Integer limit, int maxTime) {
		Bson query = filterToQuery(filter);
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
				return entity;
			}
		};
		
		return Streams.stream(enrichedIterator);
	}

	@Override
	public void remove(Filter filter) {
		collection.deleteMany(filterToQuery(filter));
	}

	@Override
	public T save(T entity) {
		jongoCollection.save(entity);
		return entity;
	}

	@Override
	public void save(Iterable<T> entities) {
		jongoCollection.insert(StreamSupport.stream(entities.spliterator(), false).toArray());
	}

	@Override
	public void createOrUpdateIndex(String field) {
		createOrUpdateIndex(collection, field);
	}

	@Override
	public void createOrUpdateCompoundIndex(String... fields) {
		createOrUpdateCompoundIndex(fields);
	}
	
	
	public static void createOrUpdateIndex(com.mongodb.client.MongoCollection<?> collection, String attribute) {
		Document index = getIndex(collection, attribute);
		if(index==null) {
			collection.createIndex(new Document(attribute,1));
		}
	}

	public static void createOrUpdateCompoundIndex(com.mongodb.client.MongoCollection<?> collection, String... attribute) {
		Document index = getIndex(collection, attribute);
		
		if(index==null) {
			Document newIndex = new Document();
			
			for(String s : attribute)
				newIndex.append(s, 1);

			collection.createIndex(newIndex);
		}
	}
	
	private static Document getIndex(com.mongodb.client.MongoCollection<?> collection, String... attribute) {
		HashSet<String> attributes = new HashSet<>(Arrays.asList(attribute));

		for(Document index:collection.listIndexes()) {  // inspect all indexes, looking for a match
			Object o = index.get("key");

			if(o instanceof Document) {
				Document d = ((Document)o);
				
				if(attributes.equals(d.keySet())) {
					return d;
				}
			}
		}
		return null;
	}
	
//	/**
//	 * Export data to CSV
//	 * @param query
//	 * @param columns
//	 * @param writer
//	 */
//	public void export(Filter filter, Map<String, CollectionField> columns, PrintWriter writer) {
//		Bson query = filterToQuery(filter);
//		FindIterable<BasicDBObject> find = collection.find(query);
//		MongoCursor<BasicDBObject> iterator;
//		iterator = find.iterator();
//		if (!iterator.hasNext()) {
//			return;
//		}
//		BasicDBObject basicDBObject = iterator.next();
//		//if column names not provided by the caller, get them from the collection
//		if (columns == null || columns.size() == 0) {
//			columns = getExportFields();
//		}
//		//if the collection has also no specification, dump all keys found in first object
//		if (columns == null || columns.size() == 0 && iterator.hasNext()) {
//			columns = getExportFields();		
//			for (String key : basicDBObject.keySet()) {
//				columns.put(key, new CollectionField(key,key));			
//			}
//		}
//		//write headers
//		columns.values().forEach((v)->{
//			String title = v.getTitle().replaceAll("^ID", "id");
//			writer.print(title);
//			writer.print(CSV_DELIMITER);
//		});
//		writer.println();
//		//Dump first row (required when writting all keys found in 1st object)
//		dumpRow(basicDBObject,columns,writer);
//
//		int count = 1;
//		while(iterator.hasNext()) {
//			count++;
//			basicDBObject = iterator.next();
//			dumpRow(basicDBObject,columns,writer);
//		}
//	}
//	
//	private void dumpRow(BasicDBObject basicDBObject, Map<String, CollectionField> fields, PrintWriter writer) {
//		fields.forEach((key,field)->{
//			Object value = basicDBObject.get(key);
//			if (value != null) {
//				String valueStr = field.getFormat().format(value);
//				if(valueStr.contains(CSV_DELIMITER)||valueStr.contains("\n")||valueStr.contains("\"")) {
//					valueStr = "\"" + valueStr.replaceAll("\"", "\"\"") + "\"";
//				}
//				writer.print(valueStr);
//			}
//			writer.print(CSV_DELIMITER);
//		});
//		writer.println();
//	}
//
//	protected Map<String, CollectionField> getExportFields() {
//		Map<String, CollectionField> result = new HashMap<String,CollectionField> ();
//		return result;
//	}
	
}
