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

import java.util.List;

import step.plugins.datatable.formatters.Formatter;
import step.plugins.screentemplating.InputType;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ColumnDef {

	String title;
	
	String value;
	
	InputType inputType;
	
	@JsonIgnore
	Formatter format;
	
	@JsonIgnore
	SearchQueryFactory queryFactory;
	
	List<String> distinctValues;

	public ColumnDef(String title, String value, InputType inputType,
			Formatter format, SearchQueryFactory queryFactory,
			List<String> distinctValues) {
		super();
		this.title = title;
		this.value = value;
		this.inputType = inputType;
		this.format = format;
		this.queryFactory = queryFactory;
		this.distinctValues = distinctValues;
	}

	public String getTitle() {
		return title;
	}

	public String getValue() {
		return value;
	}

	public Formatter getFormat() {
		return format;
	}

	public void setFormat(Formatter format) {
		this.format = format;
	}

	public List<String> getDistinctValues() {
		return distinctValues;
	}

	public void setDistinctValues(List<String> distinctValues) {
		this.distinctValues = distinctValues;
	}

	public SearchQueryFactory getQueryFactory() {
		return queryFactory;
	}

	public void setQueryFactory(SearchQueryFactory queryFactory) {
		this.queryFactory = queryFactory;
	}

	public InputType getInputType() {
		return inputType;
	}

	public void setInputType(InputType inputType) {
		this.inputType = inputType;
	}
	
}
