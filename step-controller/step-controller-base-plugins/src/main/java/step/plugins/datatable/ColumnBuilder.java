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

import step.core.accessors.DateRangeCriterium;
import step.plugins.datatable.formatters.ArrayFormatter;
import step.plugins.datatable.formatters.DateFormatter;
import step.plugins.datatable.formatters.JsonFormatter;
import step.plugins.datatable.formatters.RowAsJsonFormatter;
import step.plugins.datatable.formatters.StringFormatter;
import step.plugins.screentemplating.InputType;

public class ColumnBuilder {

	private List<ColumnDef> columns = new ArrayList<ColumnDef>();
	
	public ColumnBuilder addColumn(String columnTitle, String value) {
		columns.add(new ColumnDef(columnTitle, value, InputType.TEXT, new StringFormatter(), new TextCriterium(), null));
		return this;
	}
	
	public ColumnBuilder addJsonColumn(String columnTitle, String value) {
		columns.add(new ColumnDef(columnTitle, value, InputType.NONE, new JsonFormatter(), null, null));
		return this;
	}
	
	public ColumnBuilder addArrayColumn(String columnTitle, String value) {
		columns.add(new ColumnDef(columnTitle, value, InputType.NONE, new ArrayFormatter(), null, null));
		return this;
	}
	
	public ColumnBuilder addTextWithDropdownColumn(String columnTitle, String value, List<String> distinct) {
		columns.add(new ColumnDef(columnTitle, value, InputType.TEXT_DROPDOWN, new StringFormatter(), new TextCriterium(), distinct));
		return this;
	}
	
	public ColumnBuilder addDateColumn(String columnTitle, String value) {
		columns.add(new ColumnDef(columnTitle, value, InputType.DATE_RANGE, new DateFormatter("dd.MM.yyyy HH:mm:ss"), new DateRangeCriterium("dd.MM.yyyy"), null));
		return this;
	}
	
	public ColumnBuilder addTimeColumn(String columnTitle, String value) {
		columns.add(new ColumnDef(columnTitle, value, InputType.TEXT, new DateFormatter("HH:mm:ss"), new DateRangeCriterium("dd.MM.yyyy"), null));
		return this;
	}
	
	public ColumnBuilder addRowAsJson(String columnTitle) {
		columns.add(new ColumnDef(columnTitle, null, InputType.NONE, new RowAsJsonFormatter(), null, null));
		return this;
	}
	
	public List<ColumnDef> build() {
		return columns;
	}
}
