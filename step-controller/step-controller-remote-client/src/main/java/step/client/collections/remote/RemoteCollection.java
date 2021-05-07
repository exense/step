package step.client.collections.remote;

import step.client.AbstractRemoteClient;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.SearchOrder;


import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Stream;

public class RemoteCollection<T extends AbstractIdentifiableObject> implements Collection<T> {

    protected final String path;
    protected final Class<T> entityClass;
    protected AbstractRemoteClient client;

    public RemoteCollection(AbstractRemoteClient client, String collection, Class<T> entityClass){
        super();
        this.path = "/rest/" + collection;
        this.entityClass = entityClass;
        this.client = client;
    }

    @Override
    public Stream<T> find(Filter filter, SearchOrder order, Integer skip, Integer limit, int maxTime) {
        FindRequest findRequest = new FindRequest(filter,order,skip,limit,maxTime);
        Invocation.Builder builder = client.requestBuilder(path + "/find");
        Entity<FindRequest> entity = Entity.entity(findRequest, MediaType.APPLICATION_JSON);
        return client.executeRequest(()->builder.post(entity,new GenericType<Stream<T>>() {}));
    }

    @Override
    public List<String> distinct(String columnName) {
        Invocation.Builder builder = client.requestBuilder(path + "/distinct/" + columnName);
        return client.executeRequest(()->builder.get(new GenericType<List<String>>() {}));
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
        return client.executeRequest(()->builder.post(entityPayload,new GenericType<T>() {}));
    }

    @Override
    public void save(Iterable<T> entities) {
        Entity<Iterable<T>> entityPayload = Entity.entity(entities, MediaType.APPLICATION_JSON);
        Invocation.Builder builder = client.requestBuilder(path + "/save");
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

    protected UnsupportedOperationException notImplemented()  {
        return new UnsupportedOperationException("This method is currently not implemented");
    }
}
