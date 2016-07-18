package step.plugins.datatable;

import java.util.ArrayList;
import java.util.List;

import step.core.accessors.Collection;
import step.plugins.datatable.formatters.ArrayFormatter;
import step.plugins.datatable.formatters.DateFormatter;
import step.plugins.datatable.formatters.JsonFormatter;
import step.plugins.datatable.formatters.RowAsJsonFormatter;
import step.plugins.datatable.formatters.StringFormatter;
import step.plugins.screentemplating.InputType;

public class BackendDataTable {
	
	private Collection collection;
	
	private List<ColumnDef> columns = new ArrayList<ColumnDef>();
	
	private List<ColumnDef> exportColumns;
	
	private CollectionQueryFactory query;

	public BackendDataTable(Collection collection) {
		super();
		this.collection = collection;
	}

	public BackendDataTable setColumns(List<ColumnDef> columns) {
		this.columns = columns;
		return this;
	}
	
	public BackendDataTable setExportColumns(List<ColumnDef> columns) {
		this.exportColumns = columns;
		return this;
	}

	public List<ColumnDef> getExportColumns() {
		return exportColumns;
	}

	public BackendDataTable addColumn(String columnTitle, String value) {
		columns.add(new ColumnDef(columnTitle, value, InputType.TEXT, new StringFormatter(), new TextCriterium(), null));
		return this;
	}
	
	
	public BackendDataTable addJsonColumn(String columnTitle, String value) {
		columns.add(new ColumnDef(columnTitle, value, InputType.NONE, new JsonFormatter(), null, null));
		return this;
	}
	
	public BackendDataTable addArrayColumn(String columnTitle, String value) {
		columns.add(new ColumnDef(columnTitle, value, InputType.NONE, new ArrayFormatter(), null, null));
		return this;
	}
	
	public BackendDataTable addTextWithDropdownColumn(String columnTitle, String value) {
		List<String> distinct = collection.distinct(value);
		return addTextWithDropdownColumn(columnTitle, value, distinct);
	}
	
	public BackendDataTable addTextWithDropdownColumn(String columnTitle, String value, List<String> options) {
		columns.add(new ColumnDef(columnTitle, value, InputType.TEXT_DROPDOWN, new StringFormatter(), new TextCriterium(), options));
		return this;
	}
	
	public BackendDataTable addDateColumn(String columnTitle, String value) {
		columns.add(new ColumnDef(columnTitle, value, InputType.DATE_RANGE, new DateFormatter("dd.MM.yyyy HH:mm:ss"), new DateRangeCriterium("dd.MM.yyyy"), null));
		return this;
	}
	
	public BackendDataTable addTimeColumn(String columnTitle, String value) {
		columns.add(new ColumnDef(columnTitle, value, InputType.TEXT, new DateFormatter("HH:mm:ss"), new DateRangeCriterium("dd.MM.yyyy"), null));
		return this;
	}
	
	public BackendDataTable addRowAsJson(String columnTitle) {
		columns.add(new ColumnDef(columnTitle, null, InputType.NONE, new RowAsJsonFormatter(), null, null));
		return this;
	}
	
	public BackendDataTable addRowAsJson(String columnTitle, String... searchAttributes) {
		columns.add(new ColumnDef(columnTitle, null, InputType.TEXT, new RowAsJsonFormatter(), new MultiTextCriterium(searchAttributes), null));
		return this;
	}
	
	public ColumnDef getColumnByID(int i) {
		return columns.get(i);
	}

	public Collection getCollection() {
		return collection;
	}

	public List<ColumnDef> getColumns() {
		return columns;
	}

	public CollectionQueryFactory getQuery() {
		return query;
	}

	public BackendDataTable setQuery(CollectionQueryFactory query) {
		this.query = query;
		return this;
	}
}
