/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.plugins.datatable;

import java.util.ArrayList;
import java.util.List;

import step.core.accessors.Collection;
import step.plugins.datatable.formatters.ArrayFormatter;
import step.plugins.datatable.formatters.DateFormatter;
import step.plugins.datatable.formatters.Formatter;
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

	public boolean isFiltered() {
		return collection.isFiltered();
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
	
	public BackendDataTable addTextWithDropdownColumnOptimized(String columnTitle, String value, List<String> options) {
		columns.add(new ColumnDef(columnTitle, value, InputType.TEXT_DROPDOWN, new StringFormatter(), new PrefixedTextCriterium(), options));
		return this;
	}
	
	public BackendDataTable addDateColumn(String columnTitle, String value) {
		columns.add(new ColumnDef(columnTitle, value, InputType.DATE_RANGE, new DateFormatter("dd.MM.yyyy HH:mm:ss"), new DateRangeCriterium("dd.MM.yyyy"), null));
		return this;
	}
	
	public BackendDataTable addDateAsEpochColumn(String columnTitle, String value) {
		columns.add(new ColumnDef(columnTitle, value, InputType.DATE_RANGE, new StringFormatter(), new DateRangeCriterium("dd.MM.yyyy"), null));
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
	
	public BackendDataTable addRowAsJson(String columnTitle, List<String> searchAttributes) {
		columns.add(new ColumnDef(columnTitle, null, InputType.TEXT, new RowAsJsonFormatter(), new MultiTextCriterium(searchAttributes), null));
		return this;
	}
	
	public BackendDataTable addCustomColumn(String columnTitle, Formatter formatter) {
		columns.add(new ColumnDef(columnTitle, null, InputType.NONE, formatter, null, null));
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
