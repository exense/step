package step.plugins.parametermanager;

import java.util.Iterator;

import org.bson.conversions.Bson;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoDatabase;

import step.core.accessors.Collection;
import step.core.accessors.CollectionFind;
import step.core.accessors.SearchOrder;

public class ParameterCollection extends Collection {

	public ParameterCollection(MongoDatabase mongoDatabase) {
		super(mongoDatabase, "parameters", Parameter.class, true);
	}

	@Override
	public CollectionFind<DBObject> find(Bson query, SearchOrder order, Integer skip, Integer limit) {
		CollectionFind<DBObject> find = super.find(query, order, skip, limit);
		
		Iterator<DBObject> iterator = find.getIterator();
		Iterator<DBObject> filteredIterator = new Iterator<DBObject>() {

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public DBObject next() {
				BasicDBObject next = (BasicDBObject) iterator.next();
				if(next.containsKey("protectedValue")&&next.getBoolean("protectedValue")) {
					next.put("value", ParameterServices.PROTECTED_VALUE);					
				}
				return next;
			}
			
		};
		CollectionFind<DBObject> filteredFind = new CollectionFind<>(find.getRecordsTotal(), find.getRecordsFiltered(), filteredIterator);
		return filteredFind;
	}

}
