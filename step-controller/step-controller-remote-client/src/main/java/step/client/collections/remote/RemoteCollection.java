package step.client.collections.remote;

import step.client.AbstractRemoteClient;
import step.commons.iterators.SkipLimitIterator;
import step.commons.iterators.SkipLimitProvider;
import ch.exense.commons.core.collections.Collection;
import ch.exense.commons.core.collections.Filter;
import ch.exense.commons.core.collections.SearchOrder;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

        ParameterizedType parameterizedGenericType = new ParameterizedType() {
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

        GenericType<List<T>> genericType = new GenericType<List<T>>(
                parameterizedGenericType) {
        };

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

    @Override
    public List<String> distinct(String columnName, Filter filter) {
        Entity<Filter> entity = Entity.entity(filter, MediaType.APPLICATION_JSON);
        Invocation.Builder builder = client.requestBuilder(path + "/distinct/" + columnName);
        return client.executeRequest(()->builder.post(entity,new GenericType<List<String>>() {}));
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
        Entity<Iterable<T>> entityPayload = Entity.entity(entities, MediaType.APPLICATION_JSON);
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
}
