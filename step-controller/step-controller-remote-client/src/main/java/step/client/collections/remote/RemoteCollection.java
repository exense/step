package step.client.collections.remote;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

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

        Iterable<T> iterable = new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new SkipLimitIterator<T>(new SkipLimitProvider<T>() {
                    @Override
                    public List<T> getBatch(int skipIterator, int limitIterator) {
                        int calculatedSkip = (skip != null) ? skip + skipIterator : skipIterator;
                        int calculatedLimit = limitIterator;
                        if (limit != null && ((calculatedSkip + limitIterator) > (skip + limit))) {
                            calculatedLimit = (skip + limit) - calculatedSkip;
                        }
                        findRequest.setSkip(calculatedSkip);
                        findRequest.setLimit(calculatedLimit);
                        Entity<FindRequest> entity = Entity.entity(findRequest, MediaType.APPLICATION_JSON);
                        List<T> ts = client.executeRequest(() -> builder.post(entity, genericType));
                        return ts;
                    }
                });
            }
        };

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
