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

public class Collection {

	MongoCollection collection;
		
	public Collection(MongoClient client, String collectionName) {
		super();
		this.collection = getCollection(client, collectionName);
	}
	
	private MongoCollection getCollection(MongoClient client, String collectionName) {
		DB db = client.getDB( "step" );
		
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
