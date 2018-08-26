package step.core.accessors;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

public interface CRUDAccessor<T extends AbstractIdentifiableObject> {

	T get(ObjectId id);

	T findByAttributes(Map<String, String> attributes);

	Iterator<T> getAll();

	void remove(ObjectId id);

	T save(T entity);

	void save(List<? extends T> entities);

}