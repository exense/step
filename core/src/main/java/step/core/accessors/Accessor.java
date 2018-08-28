package step.core.accessors;

import java.util.Iterator;
import java.util.Map;

import org.bson.types.ObjectId;

public interface Accessor<T extends AbstractIdentifiableObject> {

	T get(ObjectId id);

	T findByAttributes(Map<String, String> attributes);

	Iterator<T> getAll();
}
