package step.core.accessors;

import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;

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
	 * Get an object by id
	 * 
	 * @param id the UID of the object
	 * @return the object
	 */
	T get(String id);

	/**
	 * Find an object by default attributes. If multiple objects match these attributes, the first one will be returned
	 * 
	 * @param attributes the map of mandatory attributes of the object to be found
	 * @return the object
	 */
	T findByAttributes(Map<String, String> attributes);
	
	/**
	 * Find objects by attributes.
	 * 
	 * @param attributes the map of mandatory attributes of the object to be found
	 * @return an {@link Iterator} for the objects found
	 */
	Spliterator<T> findManyByAttributes(Map<String, String> attributes);

	Iterator<T> getAll();

	/**
	 * Find an object by attributes. If multiple objects match these attributes, the first one will be returned
	 * 
	 * @param attributes the map of mandatory attributes of the object to be found
	 * @param attributesMapKey the string representing the name (or "key") of the attribute map
	 * @return the object
	 */
	T findByAttributes(Map<String, String> attributes, String attributesMapKey);

	/**
	 * Find objects by attributes.
	 * 
	 * @param attributes the map of mandatory attributes of the object to be found
	 * @param attributesMapKey the string representing the name (or "key") of the attribute map
	 * @return an {@link Iterator} for the objects found
	 */
	Spliterator<T> findManyByAttributes(Map<String, String> attributes, String attributesMapKey);
}
