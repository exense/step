package step.ide.collections;

import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Filter;
import step.core.collections.IndexField;
import step.core.collections.Order;
import step.core.collections.SearchOrder;
import step.core.collections.inmemory.InMemoryCollection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class DynamicallyDelegatingCollection<T> implements Collection<T> {
    private final String name;
    private final Class<T> type;
    private final Collection<T> fallback = new InMemoryCollection<>();
    private final AtomicReference<Collection<T>> currentCollection = new AtomicReference<>(null);

    public DynamicallyDelegatingCollection(String name, Class<T> type, CollectionFactory currentFactory) {
        this.name = name;
        this.type = type;
        setFromCurrentFactory(currentFactory);
    }

    public void setFromCurrentFactory(CollectionFactory currentFactory) {
        if (currentFactory != null) {
            currentCollection.set(currentFactory.getCollection(name, type));
        } else {
            currentCollection.set(null);
        }
    }

    private Collection<T> current() {
        return Optional.ofNullable(currentCollection.get()).orElse(fallback);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long count(Filter filter, Integer limit) {
        return current().count(filter, limit);
    }

    @Override
    public long estimatedCount() {
        return current().estimatedCount();
    }

    @Override
    public Stream<T> find(Filter filter, SearchOrder order, Integer skip, Integer limit, int maxTime) {
        return current().find(filter, order, skip, limit, maxTime);
    }

    @Override
    public Stream<T> findLazy(Filter filter, SearchOrder order, Integer skip, Integer limit, int maxTime) {
        return current().findLazy(filter, order, skip, limit, maxTime);
    }

    @Override
    public Stream<T> findReduced(Filter filter, SearchOrder order, Integer skip, Integer limit, int maxTime, List<String> reduceFields) {
        return current().findReduced(filter, order, skip, limit, maxTime, reduceFields);
    }

    @Override
    public List<String> distinct(String columnName, Filter filter) {
        return current().distinct(columnName, filter);
    }

    @Override
    public void remove(Filter filter) {
        current().remove(filter);
    }

    @Override
    public T save(T entity) {
        return current().save(entity);
    }

    @Override
    public void save(Iterable<T> entities) {
        current().save(entities);
    }

    @Override
    public void createOrUpdateIndex(String field) {
        // no-op
    }

    @Override
    public void createOrUpdateIndex(IndexField indexField) {
        // no-op

    }

    @Override
    public void createOrUpdateIndex(String field, Order order) {
        // no-op

    }

    @Override
    public void createOrUpdateCompoundIndex(String... fields) {
        // no-op

    }

    @Override
    public void createOrUpdateCompoundIndex(LinkedHashSet linkedHashSet) {
        // no-op

    }

    @Override
    public void rename(String newName) {
        // no-op

    }

    @Override
    public void drop() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<T> getEntityClass() {
        return type;
    }

    @Override
    public void dropIndex(String indexName) {
        //no-op
    }
}
