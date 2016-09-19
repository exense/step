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
package step.core.accessors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jongo.Find;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.ResultHandler;

import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

import step.commons.conf.Configuration;

public class Collection {

	MongoCollection collection;
		
	public Collection(MongoClient client, String collectionName) {
		super();
		this.collection = getCollection(client, collectionName);
	}
	
	private MongoCollection getCollection(MongoClient client, String collectionName) {
		//TODO pass DB Object instead of MongoClient
		String database = Configuration.getInstance().getProperty("db.database","step");
		DB db = client.getDB(database);
		
		Jongo jongo = new Jongo(db);
		MongoCollection collection = jongo.getCollection(collectionName);
		
		return collection;
	}
	
	public List<String> distinct(String key) {
		return collection.distinct(key).as(String.class);
	}

	public CollectionFind<DBObject> find(List<String> queryFragments, SearchOrder order, Integer skip, Integer limit) {
		StringBuilder query = new StringBuilder();
		List<Object> parameters = new ArrayList<>();
		if(queryFragments!=null&&queryFragments.size()>0) {
			query.append("{$and:[");
			Iterator<String> it = queryFragments.iterator();
			while(it.hasNext()) {
				String criterium = it.next();
				query.append("{"+criterium+"}");
				if(it.hasNext()) {
					query.append(",");
				}
			}
			query.append("]}");
		}
		
		StringBuilder sort = new StringBuilder();
		sort.append("{").append(order.getAttributeName()).append(":")
			.append(Integer.toString(order.getOrder())).append("}");
		
		long count = collection.count();
		long countResults = collection.count(query.toString(),parameters.toArray());
		
		Find find = collection.find(query.toString(), parameters.toArray()).sort(sort.toString());
		if(skip!=null) {
			find.skip(skip);
		}
		if(limit!=null) {
			find.limit(limit);
		}
		return new CollectionFind<DBObject>(count, countResults, find.map(new ResultHandler<DBObject>() {
			@Override
			public DBObject map(DBObject result) {
				return result;
			}
		}).iterator());
	}
	
}
