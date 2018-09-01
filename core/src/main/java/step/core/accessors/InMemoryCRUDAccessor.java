package step.core.accessors;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.types.ObjectId;

public class InMemoryCRUDAccessor<T extends AbstractIdentifiableObject> implements CRUDAccessor<T> {

	protected Map<ObjectId, T> map = new ConcurrentHashMap<>();
	
	@Override
	public T get(ObjectId id) {
		return map.get(id);
	}

	@Override
	public T findByAttributes(Map<String, String> attributes) {
		return map.values().stream().filter(v->{
			if(v instanceof AbstractOrganizableObject) {
				return ((AbstractOrganizableObject)v).attributes.equals(attributes);
			} else {
				return false;
			}
		}).findFirst().orElse(null);
	}

	@Override
	public Iterator<T> getAll() {
		return map.values().iterator();
	}

	@Override
	public void remove(ObjectId id) {
		map.remove(id);
	}

	@Override
	public T save(T entity) {
		if(entity.getId()==null) {
			entity.setId(new ObjectId());
		}
		return map.put(entity.getId(), entity);
	}

	@Override
	public void save(List<? extends T> entities) {
		entities.forEach(e->save(e));
	}

}
