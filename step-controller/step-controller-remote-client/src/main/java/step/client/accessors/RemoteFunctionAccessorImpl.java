package step.client.accessors;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.bson.types.ObjectId;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.functions.Function;
import step.functions.accessor.FunctionCRUDAccessor;

public class RemoteFunctionAccessorImpl extends AbstractRemoteClient implements FunctionCRUDAccessor {

	public RemoteFunctionAccessorImpl() {
		super();
	}

	public RemoteFunctionAccessorImpl(ControllerCredentials credentials) {
		super(credentials);
	}

	@Override
	public Function get(ObjectId id) {
		Builder b = requestBuilder("/rest/functions/"+id);
		return executeRequest(()->b.get(Function.class));
	}
	
	@Override
	public Function get(String id) {
		return get(new ObjectId(id));
	}

	@Override
	public Function findByAttributes(Map<String, String> attributes) {
		Builder b = requestBuilder("/rest/functions/search");
		Entity<Map<String, String>> entity = Entity.entity(attributes, MediaType.APPLICATION_JSON);
		return  executeRequest(()->b.post(entity,Function.class));
	}

	@Override
	public Iterator<Function> getAll() {
		throw notImplemented();
	}

	@Override
	public void remove(ObjectId id) {
		Builder b = requestBuilder("/rest/functions/"+id);
		executeRequest(()->b.delete(Function.class));
	}

	@Override
	public Function save(Function function) {
		Builder b = requestBuilder("/rest/functions");
		Entity<?> entity = Entity.entity(function, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity, Function.class));
	}

	@Override
	public void save(Collection<? extends Function> entities) {
		entities.forEach(f->save(f));
	}

	@Override
	public Spliterator<Function> findManyByAttributes(Map<String, String> attributes) {
		Builder b = requestBuilder("/rest/functions/find/many");
		Entity<Map<String, String>> entity = Entity.entity(attributes, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity,new GenericType<List<Function>>() {})).spliterator();
	}

	@Override
	public Function findByAttributes(Map<String, String> attributes, String attributesMapKey) {
		throw notImplemented();
	}

	@Override
	public Spliterator<Function> findManyByAttributes(Map<String, String> attributes, String attributesMapKey) {
		throw notImplemented();
	}

}
