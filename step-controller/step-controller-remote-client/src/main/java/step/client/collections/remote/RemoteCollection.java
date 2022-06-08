package step.client.collections.remote;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;

import step.client.AbstractRemoteClient;
import step.commons.iterators.SkipLimitIterator;
import step.commons.iterators.SkipLimitProvider;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.SearchOrder;

public class RemoteCollection<T> implements Collection<T> {

    protected final String path;
    protected final Class<T> entityClass;
    protected AbstractRemoteClient client;

    public RemoteCollection(AbstractRemoteClient client, String collection, Class<T> entityClass){
        super();
        this.path = "/rest/remote/" + collection;
        this.entityClass = entityClass;
        this.client = client;
    }

    @Override
    public Stream<T> find(Filter filter, SearchOrder order, Integer skip, Integer limit, int maxTime) {
        FindRequest findRequest = new FindRequest(filter,order,skip,limit,maxTime);
        Invocation.Builder builder = client.requestBuilder(path + "/find");

        GenericType<List<T>> genericType = genericTypeForEntityList();

        Iterable<T> iterable = () -> new SkipLimitIterator<>((skipIterator, limitIterator) -> {
            int skip_value =  (skip != null) ? skip : 0;
            int limit_value =  (limit != null) ? limit : 0;

            int calculatedSkip = skip_value + skipIterator;

            int calculatedLimit = limitIterator;

            if (limit_value > 0 && calculatedSkip + limitIterator > skip_value + limit_value) {
                calculatedLimit = limit_value - calculatedSkip;
            }

            if (calculatedLimit>0) {
                findRequest.setSkip(calculatedSkip);
                findRequest.setLimit(calculatedLimit);
                Entity<FindRequest> entity = Entity.entity(findRequest, MediaType.APPLICATION_JSON);
                return client.executeRequest(() -> builder.post(entity, genericType));
            } else {
                return new ArrayList<>();
            }
        });
        return StreamSupport.stream(iterable.spliterator(), false);
    }

	private GenericType<List<T>> genericTypeForEntityList() {
		ParameterizedType parameterizedGenericType = getParametrizedTypeForEntityList();

        GenericType<List<T>> genericType = new GenericType<List<T>>(
                parameterizedGenericType) {
        };
		return genericType;
	}

	private ParameterizedType getParametrizedTypeForEntityList() {
		return new ParameterizedType() {
            public Type[] getActualTypeArguments() {
                return new Type[] { entityClass };
            }

            public Type getRawType() {
                return List.class;
            }

            public Type getOwnerType() {
                return List.class;
            }
        };
	}

    @Override
    public List<String> distinct(String columnName, Filter filter) {
        Entity<Filter> entity = Entity.entity(filter, MediaType.APPLICATION_JSON);
        Invocation.Builder builder = client.requestBuilder(path + "/distinct/" + columnName);
        return client.executeRequest(()->builder.post(entity,new GenericType<List<String>>() {}));
    }

    @Override
    public Stream<T> findReduced(Filter filter, SearchOrder order, Integer skip, Integer limit, int maxTime, List<String> reduceFields) {
        throw new UnsupportedOperationException("This method is currently not implemented");
    }

    @Override
    public void remove(Filter filter) {
        Entity<Filter> entity = Entity.entity(filter, MediaType.APPLICATION_JSON);
        Invocation.Builder builder = client.requestBuilder(path + "/remove");
        client.executeRequest(()->builder.post(entity));
    }

    @Override
    public T save(T entity) {
        Entity<T> entityPayload = Entity.entity(entity, MediaType.APPLICATION_JSON);
        Invocation.Builder builder = client.requestBuilder(path + "/save");
        return client.executeRequest(()->builder.post(entityPayload, entityClass));
    }

    @Override
    public void save(Iterable<T> entities) {
    	List<T> arrayList = new ArrayList<T>();
    	entities.forEach(e->arrayList.add(e));
    	GenericEntity<List<T>> list = new GenericEntity<List<T>>(arrayList, getParametrizedTypeForEntityList());
        Entity<GenericEntity<List<T>>> entityPayload = Entity.entity(list, MediaType.APPLICATION_JSON);
        Invocation.Builder builder = client.requestBuilder(path + "/saveMany");
        client.executeRequest(()->builder.post(entityPayload));
    }

    @Override
    public void createOrUpdateIndex(String field) {
        throw notImplemented();
    }

    @Override
    public void createOrUpdateCompoundIndex(String... fields) {
        throw notImplemented();
    }

    @Override
    public void rename(String newName) {
        throw notImplemented();
    }

    @Override
    public void drop() {
        throw notImplemented();
    }

    protected UnsupportedOperationException notImplemented()  {
        return new UnsupportedOperationException("This method is currently not implemented");
    }

	@Override
	public long count(Filter filter, Integer limit) {
        Invocation.Builder builder = client.requestBuilder(path + "/count");
        Entity<CountRequest> entity = Entity.entity(new CountRequest(filter, limit), MediaType.APPLICATION_JSON);
        return client.executeRequest(()->builder.post(entity, CountResponse.class)).getCount();
	}

	@Override
	public long estimatedCount() {
		Invocation.Builder builder = client.requestBuilder(path + "/count/estimated");
        return client.executeRequest(()->builder.get(CountResponse.class)).getCount();
	}
}
