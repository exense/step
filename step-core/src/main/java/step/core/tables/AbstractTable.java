package step.core.tables;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.json.JsonObject;

import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.collections.SearchOrder;

public class AbstractTable<T> implements Table<T> {
	
	private final boolean filtered;
	protected Collection<T> collectionDriver;

	public AbstractTable(Collection<T> CollectionDriver, boolean filtered) {
		super();
		this.filtered = filtered;
		this.collectionDriver = CollectionDriver;
	}

	@Override
	public List<String> distinct(String columnName, Filter filter) {
		return collectionDriver.distinct(columnName, filter);
	}

	@Override
	public List<String> distinct(String columnName) {
		return collectionDriver.distinct(columnName);
	}

	@Override
	public TableFindResult<T> find(Filter filter, SearchOrder order, Integer skip, Integer limit, int maxTime) {
		Iterator<T> iterator = collectionDriver.find(filter, order, skip, limit, maxTime).map(this::enrichEntity).iterator();
		TableFindResult<T> enrichedResult = new TableFindResult<T>(0, 0, iterator);
		return enrichedResult;
	}

	@Override
	public Filter getQueryFragmentForColumnSearch(String columnName, String searchValue) {
		return Filters.regex(columnName, searchValue, false);
	}

	@Override
	public List<Filter> getAdditionalQueryFragments(JsonObject queryParameters) {
		return null;
	}

	@Override
	public boolean isFiltered() {
		return filtered;
	}

	@Override
	public void export(Filter query, Map<String, TableColumn> columns, PrintWriter writer) {
		
	}

	@Override
	public T enrichEntity(T entity) {
		return entity;
	}
}
