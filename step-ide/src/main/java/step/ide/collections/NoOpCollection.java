package step.ide.collections;

import step.core.collections.AbstractCollection;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.IndexField;
import step.core.collections.Order;
import step.core.collections.SearchOrder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

public class NoOpCollection<T> extends AbstractCollection<T> implements Collection<T> {
    private final String name;
    private final Class<T> type;

    public NoOpCollection(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long count(Filter filter, Integer integer) {
        return 0;
    }

    @Override
    public long estimatedCount() {
        return 0;
    }

    @Override
    public Stream<T> find(Filter filter, SearchOrder searchOrder, Integer integer, Integer integer1, int i) {
        return Stream.empty();
    }

    @Override
    public Stream<T> findLazy(Filter filter, SearchOrder searchOrder, Integer integer, Integer integer1, int i) {
        return Stream.empty();
    }

    @Override
    public Stream<T> findReduced(Filter filter, SearchOrder searchOrder, Integer integer, Integer integer1, int i, List<String> list) {
        return Stream.empty();
    }

    @Override
    public List<String> distinct(String s, Filter filter) {
        return List.of();
    }

    @Override
    public void remove(Filter filter) {

    }

    @Override
    public T save(T t) {
        return t;
    }

    @Override
    public void save(Iterable<T> iterable) {

    }

    @Override
    public void createOrUpdateIndex(String s) {

    }

    @Override
    public void createOrUpdateIndex(IndexField indexField) {

    }

    @Override
    public void createOrUpdateIndex(String s, Order order) {

    }

    @Override
    public void createOrUpdateCompoundIndex(String... strings) {

    }

    @Override
    public void createOrUpdateCompoundIndex(LinkedHashSet<IndexField> linkedHashSet) {

    }

    @Override
    public void rename(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void drop() {
    }

    @Override
    public Class<T> getEntityClass() {
        return type;
    }

    @Override
    public void dropIndex(String s) {

    }
}
