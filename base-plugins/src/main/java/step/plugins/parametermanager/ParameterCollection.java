package step.plugins.parametermanager;

import java.util.Iterator;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoDatabase;

import step.core.accessors.Collection;
import step.core.accessors.CollectionFind;
import step.core.accessors.SearchOrder;

public class ParameterCollection extends Collection {

	public ParameterCollection(MongoDatabase mongoDatabase) {
		super(mongoDatabase, "parameters");
	}

	@Override
	public CollectionFind<Document> find(Bson query, SearchOrder order, Integer skip, Integer limit) {
		CollectionFind<Document> find = super.find(query, order, skip, limit);
		
		Iterator<Document> iterator = find.getIterator();
		Iterator<Document> filteredIterator = new Iterator<Document>() {

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Document next() {
				Document next = iterator.next();
				if(next.containsKey("protectedValue")&&next.getBoolean("protectedValue")) {
					next.put("value", ParameterServices.PROTECTED_VALUE);					
				}
				return next;
			}
			
		};
		CollectionFind<Document> filteredFind = new CollectionFind<>(find.getRecordsTotal(), find.getRecordsFiltered(), filteredIterator);
		return filteredFind;
	}

}
