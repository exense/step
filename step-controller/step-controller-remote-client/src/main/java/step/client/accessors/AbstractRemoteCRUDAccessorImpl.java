package step.client.accessors;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;

import org.bson.types.ObjectId;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.CRUDAccessor;

public class AbstractRemoteCRUDAccessorImpl<T extends AbstractIdentifiableObject> extends AbstractRemoteClient implements CRUDAccessor<T>{

	private final String path;
	private final Class<T> entityClass;
	
	public AbstractRemoteCRUDAccessorImpl(String path, Class<T> entityClass) {
		super();
		this.path = path;
		this.entityClass = entityClass;
	}
	
	public AbstractRemoteCRUDAccessorImpl(ControllerCredentials credentials, String path, Class<T> entityClass) {
		super(credentials);
		this.path = path;
		this.entityClass = entityClass;
	}

	@Override
	public T get(ObjectId id) {
		Builder b = requestBuilder(path+id.toString());
		return executeRequest(()->b.get(entityClass));
	}

	@Override
	public T get(String id) {
		return get(new ObjectId(id));
	}

	@Override
	public T findByAttributes(Map<String, String> attributes) {
		Builder b = requestBuilder(path+"search");
		Entity<Map<String, String>> entity = Entity.entity(attributes, MediaType.APPLICATION_JSON);
		return  executeRequest(()->b.post(entity,entityClass));
	}

	@Override
	public Spliterator<T> findManyByAttributes(Map<String, String> attributes) {
		throw notImplemented();
	}

	@Override
	public Iterator<T> getAll() {
		throw notImplemented();
	}

	@Override
	public T findByAttributes(Map<String, String> attributes, String attributesMapKey) {
		throw notImplemented();
	}

	@Override
	public Spliterator<T> findManyByAttributes(Map<String, String> attributes, String attributesMapKey) {
		throw notImplemented();
	}

	@Override
	public void remove(ObjectId id) {
		Builder b = requestBuilder(path+id);
		executeRequest(()->b.delete(entityClass));
	}

	@Override
	public T save(T e) {
		Builder b = requestBuilder(path);
		Entity<?> entity = Entity.entity(e, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity, entityClass));
	}

	@Override
	public void save(Collection<? extends T> entities) {
		entities.forEach(e->save(e));
	}

}
