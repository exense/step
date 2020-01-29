package step.core.accessors;

import java.util.Collection;

import org.bson.types.ObjectId;

public interface CRUDAccessor<T extends AbstractIdentifiableObject> extends Accessor<T> {

	void remove(ObjectId id);

	/**
	 * Save an entity. If an entity with the same id exists, it will be updated otherwise inserted. 
	 * 
	 * @param entity the entity instance to be saved
	 * @return the saved entity
	 */
	T save(T entity);

	/**
	 * Save a list of entities. 
	 * 
	 * @param entities the list of entities to be saved
	 */
	void save(Collection<? extends T> entities);

}