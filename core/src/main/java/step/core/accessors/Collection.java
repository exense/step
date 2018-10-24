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
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CountOptions;

import step.commons.conf.Configuration;

public class Collection {

	MongoCollection<Document> collection;

	public Collection(MongoDatabase mongoDatabase, String collectionName) {
		super();
		collection = mongoDatabase.getCollection(collectionName);
	}
	
	public List<String> distinct(String key) {
		return collection.distinct(key, String.class).filter(new Document(key,new Document("$ne",null))).into(new ArrayList<String>());
	}

	public CollectionFind<Document> find(Bson query, SearchOrder order, Integer skip, Integer limit) {
//		StringBuilder query = new StringBuilder();
//		List<Object> parameters = new ArrayList<>();
//		if(queryFragments!=null&&queryFragments.size()>0) {
//			query.append("{$and:[");
//			Iterator<String> it = queryFragments.iterator();
//			while(it.hasNext()) {
//				String criterium = it.next();
//				query.append("{"+criterium+"}");
//				if(it.hasNext()) {
//					query.append(",");
//				}
//			}
//			query.append("]}");
//		}
		
//		StringBuilder sort = new StringBuilder();
//		sort.append("{").append(order.getAttributeName()).append(":")
//			.append(Integer.toString(order.getOrder())).append("}");
		
		long count = collection.count();
		
		int countLimit = Configuration.getInstance().getPropertyAsInteger("db.collection.count.limit.default", 1000);
		CountOptions option = new CountOptions();
		option.skip(0).limit(countLimit);
		long countResults = collection.count(query, option);
		
		FindIterable<Document> find = collection.find(query);
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
		return new CollectionFind<Document>(count, countResults, find.iterator());
	}
	
}
