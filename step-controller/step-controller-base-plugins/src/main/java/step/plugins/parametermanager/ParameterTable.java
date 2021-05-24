/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.plugins.parametermanager;

import java.util.Iterator;
import java.util.List;

import ch.exense.commons.core.collections.Collection;
import ch.exense.commons.core.collections.Filter;
import ch.exense.commons.core.collections.Filters;
import ch.exense.commons.core.collections.SearchOrder;
import step.core.tables.AbstractTable;
import step.core.tables.TableFindResult;
import step.parameter.Parameter;

public class ParameterTable extends AbstractTable<Parameter> {

	private static final String PARAMETER_FIELD_PRIORITY = "priority";
	private static final String PARAMETER_FIELD_SCOPE_ENTITY = "scopeEntity";
	private static final String PARAMETER_FIELD_SCOPE = "scope";

	public ParameterTable(Collection<Parameter> collection) {
		super(collection, true);
	}

	@Override
	public TableFindResult<Parameter> find(Filter query, SearchOrder order, Integer skip, Integer limit, int maxTime) {
		TableFindResult<Parameter> find = super.find(query, order, skip, limit, maxTime);
		
		Iterator<Parameter> iterator = find.getIterator();
		Iterator<Parameter> filteredIterator = new Iterator<Parameter>() {

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Parameter next() {
				Parameter next = iterator.next();
				return ParameterServices.maskProtectedValue(next);
			}
			
		};
		TableFindResult<Parameter> filteredFind = new TableFindResult<>(find.getRecordsTotal(), find.getRecordsFiltered(), filteredIterator);
		return filteredFind;
	}
	
	@Override
	public Filter getQueryFragmentForColumnSearch(String columnName, String searchValue) {
		if (columnName.equals(PARAMETER_FIELD_SCOPE)) {
			// The column is displaying Scope displays the scope and the entity related to it
			// We're therefore creating a composite filter on these 2 fields
			return Filters.or(List.of(super.getQueryFragmentForColumnSearch(PARAMETER_FIELD_SCOPE, searchValue),
					super.getQueryFragmentForColumnSearch(PARAMETER_FIELD_SCOPE_ENTITY, searchValue)));
		} else if (columnName.equals(PARAMETER_FIELD_PRIORITY)) {
			// The field priority is stored as a number. Regexp filters are therefore not working
			// For this reason one have to Filter using the eq filter
			int parseInt;
			try {
				parseInt = Integer.parseInt(searchValue);
				return Filters.equals(PARAMETER_FIELD_PRIORITY, parseInt);
			} catch (NumberFormatException e) {
				return super.getQueryFragmentForColumnSearch(PARAMETER_FIELD_PRIORITY, searchValue);
			}
		} else {
			return super.getQueryFragmentForColumnSearch(columnName, searchValue);
		}
	}

}
