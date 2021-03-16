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

import org.bson.conversions.Bson;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import step.core.accessors.collections.Collection;
import step.core.accessors.collections.CollectionFind;
import step.core.accessors.collections.SearchOrder;
import step.parameter.Parameter;

public class ParameterCollection extends Collection<Parameter> {

	private static final String COLLECTION_PARAMETERS = "parameters";
	
	private static final String PARAMETER_FIELD_PRIORITY = "priority";
	private static final String PARAMETER_FIELD_SCOPE_ENTITY = "scopeEntity";
	private static final String PARAMETER_FIELD_SCOPE = "scope";

	public ParameterCollection(MongoDatabase mongoDatabase) {
		super(mongoDatabase, COLLECTION_PARAMETERS, Parameter.class, true);
	}
	
	@Override
	public CollectionFind<Parameter> find(Bson query, SearchOrder order, Integer skip, Integer limit, int maxTime) {
		CollectionFind<Parameter> find = super.find(query, order, skip, limit, maxTime);
		
		Iterator<Parameter> iterator = find.getIterator();
		Iterator<Parameter> filteredIterator = new Iterator<Parameter>() {

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Parameter next() {
				Parameter next = iterator.next();
				if(next.getProtectedValue()) {
					next.setValue(ParameterServices.PROTECTED_VALUE);					
				}
				return next;
			}
			
		};
		CollectionFind<Parameter> filteredFind = new CollectionFind<>(find.getRecordsTotal(), find.getRecordsFiltered(), filteredIterator);
		return filteredFind;
	}
	
	@Override
	public Bson getQueryFragmentForColumnSearch(String columnName, String searchValue) {
		if (columnName.equals(PARAMETER_FIELD_SCOPE)) {
			// The column is displaying Scope displays the scope and the entity related to it
			// We're therefore creating a composite filter on these 2 fields
			return Filters.or(super.getQueryFragmentForColumnSearch(PARAMETER_FIELD_SCOPE, searchValue),
					super.getQueryFragmentForColumnSearch(PARAMETER_FIELD_SCOPE_ENTITY, searchValue));
		} else if (columnName.equals(PARAMETER_FIELD_PRIORITY)) {
			// The field priority is stored as a number. Regexp filters are therefore not working
			// For this reason one have to Filter using the eq filter
			int parseInt;
			try {
				parseInt = Integer.parseInt(searchValue);
				return Filters.eq(PARAMETER_FIELD_PRIORITY, parseInt);
			} catch (NumberFormatException e) {
				return super.getQueryFragmentForColumnSearch(PARAMETER_FIELD_PRIORITY, searchValue);
			}
		} else {
			return super.getQueryFragmentForColumnSearch(columnName, searchValue);
		}
	}

}
