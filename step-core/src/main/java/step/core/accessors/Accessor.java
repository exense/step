package step.core.accessors;

import java.util.Iterator;
import java.util.Map;

import org.bson.types.ObjectId;

public interface Accessor<T extends AbstractIdentifiableObject> {

	/**
	 * Get an object by id
	 * 
	 * @param id the UID of the object
	 * @return the object
	 */
	T get(ObjectId id);

	/**
	 * Find an object by attributes. If multiple objects match these attributes, the first one will be returned
	 * 
	 * @param attributes the map of mandatory attributes of the object to be found
	 * @return the object
	 */
	T findByAttributes(Map<String, String> attributes);

	Iterator<T> getAll();
}
