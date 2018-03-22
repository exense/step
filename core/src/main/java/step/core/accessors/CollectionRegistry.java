package step.core.accessors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionRegistry {

	private Map<String, Collection> collections = new ConcurrentHashMap<>();
	
	public void register(String collectionName, Collection collection) {
		collections.put(collectionName, collection);
	}
	
	public Collection get(String collectionName) {
		return collections.get(collectionName);
	}
}
