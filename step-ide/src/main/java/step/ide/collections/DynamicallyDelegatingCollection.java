package step.ide.collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Filter;
import step.core.collections.IndexField;
import step.core.collections.Order;
import step.core.collections.SearchOrder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class DynamicallyDelegatingCollection<T> implements Collection<T> {
    private final String name;
    private final Class<T> type;
    private final Class<?> sourceType;
    private final Collection<T> fallback;
    private final AtomicReference<Collection<T>> currentCollection = new AtomicReference<>(null);
    private final ObjectMapper mapper = DefaultJacksonMapperProvider.getObjectMapper();

    public DynamicallyDelegatingCollection(String name, Class<T> type, CollectionFactory currentFactory) {
        this(name, type, type, currentFactory);
    }

    /**
     * @param type       the class of the entities this collection returns
     * @param sourceType the class the collection of the current factory holds its entities as. When it differs from
     *                   the type, a parent class of it for instance, the entities are converted when read
     */
    public DynamicallyDelegatingCollection(String name, Class<T> type, Class<?> sourceType, CollectionFactory currentFactory) {
        this.name = name;
        this.type = type;
        this.sourceType = sourceType;
        fallback = new NoOpCollection<>(name, type);
        setFromCurrentFactory(currentFactory);
    }

    @SuppressWarnings("unchecked")
    public void setFromCurrentFactory(CollectionFactory currentFactory) {
        if (currentFactory != null) {
            currentCollection.set((Collection<T>) currentFactory.getCollection(name, sourceType));
        } else {
            currentCollection.set(null);
        }
    }

    private Stream<T> toType(Stream<T> entities) {
        if (sourceType == type) {
            return entities;
        }
        return entities.map(entity -> {
            Object source = entity;
            return type.isInstance(source) ? entity : mapper.convertValue(source, type);
        });
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
        return toType(current().find(filter, order, skip, limit, maxTime));
    }

    @Override
    public Stream<T> findLazy(Filter filter, SearchOrder order, Integer skip, Integer limit, int maxTime) {
        return toType(current().findLazy(filter, order, skip, limit, maxTime));
    }

    @Override
    public Stream<T> findReduced(Filter filter, SearchOrder order, Integer skip, Integer limit, int maxTime, List<String> reduceFields) {
        return toType(current().findReduced(filter, order, skip, limit, maxTime, reduceFields));
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
