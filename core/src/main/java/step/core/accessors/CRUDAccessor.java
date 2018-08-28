package step.core.accessors;

import java.util.List;

import org.bson.types.ObjectId;

public interface CRUDAccessor<T extends AbstractIdentifiableObject> extends Accessor<T> {

	void remove(ObjectId id);

	T save(T entity);

	void save(List<? extends T> entities);

}