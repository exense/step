package step.plugins.parametermanager;

import java.util.Iterator;

import org.bson.conversions.Bson;

import com.mongodb.client.MongoDatabase;

import step.core.accessors.collections.Collection;
import step.core.accessors.collections.CollectionFind;
import step.core.accessors.collections.SearchOrder;

public class ParameterCollection extends Collection<Parameter> {

	public ParameterCollection(MongoDatabase mongoDatabase) {
		super(mongoDatabase, "parameters", Parameter.class, true);
	}

	@Override
	public CollectionFind<Parameter> find(Bson query, SearchOrder order, Integer skip, Integer limit) {
		CollectionFind<Parameter> find = super.find(query, order, skip, limit);
		
		Iterator<Parameter> iterator = find.getIterator();
		Iterator<Parameter> filteredIterator = new Iterator<Parameter>() {

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Parameter next() {
				Parameter next = iterator.next();
				if(next.getProtectedValue()) {
					next.setValue(ParameterServices.PROTECTED_VALUE);					
				}
				return next;
			}
			
		};
		CollectionFind<Parameter> filteredFind = new CollectionFind<>(find.getRecordsTotal(), find.getRecordsFiltered(), filteredIterator);
		return filteredFind;
	}

}
